package org.agito.activiti.jobexecutor.ra.impl.work;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkRejectedException;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.jobexecutor.AcquiredJobs;
import org.agito.activiti.jobexecutor.JobExecutorEE;
import org.agito.activiti.jobexecutor.JobWasAddedCallback;
import org.agito.activiti.jobexecutor.config.JobConfigurationSection;
import org.agito.activiti.jobexecutor.ra.JobExecutorResourceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobAcquisitionWork implements Work {

	private final static Logger LOGGER = LoggerFactory.getLogger(JobAcquisitionWork.class);

	protected final static long SCHEDULE_WORK_START_TIMEOUT = 2000;

	protected final String name;
	protected final Map<String, JobExecutorEE> jobExecutors;
	protected final JobConfigurationSection configuration;
	protected final JobWasAddedCallback jobWasAddedCallback;

	protected final Stack<JobExecutorEE> jobExecutorStack = new Stack<JobExecutorEE>();

	protected final JobExecutorResourceAdapter resourceAdapter;
	protected final RetryWorkListener acquisitionRetryWorkListener;

	protected volatile boolean isActive = false;
	protected volatile boolean isInterrupted = false;
	protected volatile boolean isJobAdded = false;

	protected final Object MONITOR = new Object();

	protected final AtomicBoolean isWaiting = new AtomicBoolean(false);

	protected long millisToWait = 0;
	protected float waitIncreaseFactor = 2;
	protected long maxWait = 60 * 1000;

	public JobAcquisitionWork(String name, JobConfigurationSection configuration,
			JobExecutorResourceAdapter resourceAdapter) {
		this.name = name;
		this.jobExecutors = Collections.synchronizedMap(new LinkedHashMap<String, JobExecutorEE>());
		this.configuration = configuration;
		this.resourceAdapter = resourceAdapter;
		this.acquisitionRetryWorkListener = new RetryWorkListener(resourceAdapter);
		this.jobWasAddedCallback = new JobWasAddedCallback() {

			@Override
			public void jobWasAdded() {
				JobAcquisitionWork.this.jobWasAdded();
			}

		};

		LOGGER.info("{} Job acquisition {} created. default={}", new Object[] { name, configuration.getName(),
				configuration.isDefault() });
	}

	@Override
	public void run() {

		boolean jobExecutionFailed = false;
		Stack<JobExecutorEE> activeJobExecutorsStack = new Stack<JobExecutorEE>();

		// synchronized to avoid side effects by registration/detachment
		synchronized (jobExecutors) {
			if (!isInterrupted) {
				LOGGER.debug(name + " job acquisition thread starts.");
			} else {
				LOGGER.debug(name + " job executor not active anymore. cancelling job acquisition thread.");
				return;
			}

			isJobAdded = false;

			if (jobExecutorStack.size() == 0) {
				jobExecutorStack.addAll(jobExecutors.values());
			}

		}

		while (!jobExecutorStack.empty()) {
			JobExecutorEE jobExecutor = jobExecutorStack.pop();

			final CommandExecutor commandExecutor = jobExecutor.getCommandExecutor();

			int maxJobsPerAcquisition = jobExecutor.getMaxJobsPerAcquisition();

			try {
				AcquiredJobs acquiredJobs = commandExecutor.execute(jobExecutor.getAcquireJobsCmd());

				for (List<String> jobIds : acquiredJobs.getJobIdBatches()) {
					// get jobs done by work manager thread, block until workManager starts running
					JobExecutionWork jobExecution = new JobExecutionWork(resourceAdapter, jobIds, commandExecutor);
					LOGGER.trace("{} start work for jobIds {}", new Object[] { name, jobIds });
					try {
						resourceAdapter.getBootstrapCtx().getWorkManager()
								.startWork(jobExecution, SCHEDULE_WORK_START_TIMEOUT, null, null);
					} catch (WorkRejectedException e) {
						LOGGER.warn("WorkManager rejected JobExecution with "
								+ e.getMessage()
								+ " > Code "
								+ e.getErrorCode()
								+ ". Consider reconfiguration of pool/queue sizes, in case this is recurrent. Job will be executed within acquisition thread.");
						jobExecution.run();
					}

				}

				// if all jobs were executed
				int jobsAcquired = acquiredJobs.getJobIdBatches().size();
				if (jobsAcquired >= maxJobsPerAcquisition) {
					activeJobExecutorsStack.push(jobExecutor);
				}

			} catch (Exception e) {

				LOGGER.error(name + " exception during job acquisition: " + e.getMessage(), e);

				jobExecutionFailed = true;
				activeJobExecutorsStack.push(jobExecutor);

				// if one of the engines fails: increase the wait time
				if (millisToWait == 0) {
					millisToWait = jobExecutor.getWaitTimeInMillis();
				} else {
					millisToWait *= waitIncreaseFactor;
					if (millisToWait > maxWait) {
						millisToWait = maxWait;
					}
				}

			}
		}

		if (activeJobExecutorsStack.size() == 0) {
			// none of the registered executors have jobs at the moment >> wait
			millisToWait = configuration.getWaitTimeInMillis();
		} else {
			if (!jobExecutionFailed) {
				millisToWait = 0;
			}

			// transfer active jobExecutors
			while (!activeJobExecutorsStack.empty()) {
				jobExecutorStack.push(activeJobExecutorsStack.pop());
			}
		}

		if ((millisToWait > 0) && (!isJobAdded)) {
			try {
				LOGGER.debug(name + " job acquisition thread sleeping for " + millisToWait + " millis");
				synchronized (MONITOR) {
					if (!isInterrupted) {
						isWaiting.set(true);
						MONITOR.wait(millisToWait);
					}
				}
				LOGGER.debug(name + " job acquisition thread woke up");
				isJobAdded = false;
			} catch (InterruptedException e) {
				LOGGER.debug(name + " job acquisition wait interrupted");
			} finally {
				isWaiting.set(false);
			}
		}

		if (!isInterrupted) {
			LOGGER.debug(name + " job acquisition thread restarting.");
			restartAcquisition();
		}
	}

	@Override
	public void release() {
		// ignore
	}

	protected void restartAcquisition() {
		try {
			// use scheduleWork for restarting the acquisition thread and add a retry workListener to ensure proper handling upon workRejectedException.
			// This may occur when no more slots are available in the work manager queue.
			resourceAdapter.getBootstrapCtx().getWorkManager()
					.scheduleWork(this, SCHEDULE_WORK_START_TIMEOUT, null, acquisitionRetryWorkListener);
		} catch (WorkRejectedException e) {
			LOGGER.debug(name + " exception during scheduleWork of job acquisition: " + e.getMessage(), e);
			LOGGER.warn(name + " JobAcquisitionWork rejected: " + e.getMessage());
		} catch (WorkException e) {
			LOGGER.error(name + " exception during scheduleWork of job acquisition: " + e.getMessage(), e);
		}
	}

	protected void start() {
		LOGGER.trace("start()");
		isInterrupted = false;
		restartAcquisition();
		isActive = true;
	}

	protected void stop() {
		LOGGER.trace("stop()");
		synchronized (MONITOR) {
			isInterrupted = true;
			if (isWaiting.compareAndSet(true, false)) {
				MONITOR.notifyAll();
			}
		}
		isActive = false;
	}

	// registration / detachment

	public synchronized void registerJobExecutor(JobExecutorEE jobExecutorEE) {

		synchronized (jobExecutors) {
			if (jobExecutors.containsKey(jobExecutorEE.getName()))
				throw new ActivitiException("Job executor " + jobExecutorEE.getName() + " already registered.");

			// propagate properties
			jobExecutorEE.setLockTimeInMillis(configuration.getLockTimeInMillis());
			jobExecutorEE.setMaxJobsPerAcquisition(configuration.getMaxJobsPerAcquisition());
			jobExecutorEE.setWaitTimeInMillis(configuration.getWaitTimeInMillis());

			// jobWasAdded callback
			jobExecutorEE.setJobWasAddedCallback(jobWasAddedCallback);

			jobExecutors.put(jobExecutorEE.getName(), jobExecutorEE);

			LOGGER.debug(name + " " + jobExecutorEE.getName() + " registered.");

			if (!isActive) {
				start();
			}

		}

	}

	public synchronized void detachJobExecutor(JobExecutorEE jobExecutorEE) {

		synchronized (jobExecutors) {
			if (!jobExecutors.containsKey(jobExecutorEE.getName()))
				throw new ActivitiException("Job executor " + jobExecutorEE.getName() + " not registered.");

			// jobWasAdded callback
			jobExecutorEE.setJobWasAddedCallback(null);

			jobExecutors.remove(jobExecutorEE.getName());

			LOGGER.debug(name + " " + jobExecutorEE.getName() + " deregistered.");

			if (isActive && jobExecutors.size() == 0) {
				stop();
			}
		}
	}

	public void stopAcqisition() {

		synchronized (jobExecutors) {
			if (isActive) {
				for (JobExecutorEE jobExecutor : jobExecutors.values()) {
					// jobWasAdded callback
					jobExecutor.setJobWasAddedCallback(null);
				}
				jobExecutors.clear();
				stop();
			}
			LOGGER.info("{} Job acquisition stopped.", new Object[] { name });
		}

	}

	public void jobWasAdded() {
		LOGGER.debug(name + " jobWasAdded notification");
		isJobAdded = true;
		if (isWaiting.compareAndSet(true, false)) {
			// ensures we only notify once
			// I am OK with the race condition
			synchronized (MONITOR) {
				MONITOR.notifyAll();
			}
		}
	}

	public long getMillisToWait() {
		return millisToWait;
	}

	public void setMillisToWait(long millisToWait) {
		this.millisToWait = millisToWait;
	}

	public float getWaitIncreaseFactor() {
		return waitIncreaseFactor;
	}

	public void setWaitIncreaseFactor(float waitIncreaseFactor) {
		this.waitIncreaseFactor = waitIncreaseFactor;
	}

	public long getMaxWait() {
		return maxWait;
	}

	public void setMaxWait(long maxWait) {
		this.maxWait = maxWait;
	}

	private static final class RetryWorkListener implements WorkListener {

		protected final JobExecutorResourceAdapter resourceAdapter;

		public RetryWorkListener(JobExecutorResourceAdapter resourceAdapter) {
			this.resourceAdapter = resourceAdapter;
		}

		@Override
		public void workAccepted(WorkEvent arg0) {
			// do nothing
		}

		@Override
		public void workCompleted(WorkEvent arg0) {
			// do nothing
		}

		@Override
		public void workRejected(WorkEvent event) {
			LOGGER.warn("Work {} rejected with code {}. Will retry.", new Object[] {
					event.getWork().getClass().getName(), event.getType() });
			try {
				resourceAdapter.getBootstrapCtx().getWorkManager()
						.scheduleWork(event.getWork(), SCHEDULE_WORK_START_TIMEOUT, null, this);
			} catch (WorkException e) {
				LOGGER.error(
						"exception during scheduleWork of work " + event.getWork().getClass().getName() + ": "
								+ e.getMessage(), e);
			}
		}

		@Override
		public void workStarted(WorkEvent arg0) {
			// do nothing
		}

	}

}
