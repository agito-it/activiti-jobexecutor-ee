package org.agito.activiti.jobexecutor.impl;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;

import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.jobexecutor.AcquiredJobs;
import org.activiti.engine.impl.jobexecutor.GetUnlockedTimersByDuedateCmd;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.impl.util.ClockUtil;
import org.agito.activiti.JobExecutorEE;
import org.agito.activiti.jobexecutor.JobExecutorResourceAdapter;

public class JobAcquisitionWork implements Work {

	private final static Logger LOGGER = Logger.getLogger(JobAcquisitionWork.class.getName());

	protected final JobExecutorEE jobExecutor;
	protected final JobExecutorResourceAdapter resourceAdapter;
	protected final RetryWorkListener retryWorkListener;

	protected volatile boolean isInterrupted = false;
	protected volatile boolean isJobAdded = false;

	protected final Object MONITOR = new Object();

	protected final AtomicBoolean isWaiting = new AtomicBoolean(false);

	protected long millisToWait = 0;
	protected float waitIncreaseFactor = 2;
	protected long maxWait = 60 * 1000;

	public JobAcquisitionWork(JobExecutorEE jobExecutor, JobExecutorResourceAdapter resourceAdapter) {
		this.jobExecutor = jobExecutor;
		this.resourceAdapter = resourceAdapter;
		this.retryWorkListener = new RetryWorkListener(resourceAdapter);
	}

	@Override
	public void run() {

		if (!isInterrupted) {
			LOGGER.info(jobExecutor.getName() + " job acquisition thread starts.");
		} else {
			LOGGER.info(jobExecutor.getName() + " job executor not active anymore. cancelling job acquisition thread.");
			return;
		}

		final CommandExecutor commandExecutor = jobExecutor.getCommandExecutor();

		int maxJobsPerAcquisition = jobExecutor.getMaxJobsPerAcquisition();

		try {
			AcquiredJobs acquiredJobs = commandExecutor.execute(jobExecutor.getAcquireJobsCmd());

			for (List<String> jobIds : acquiredJobs.getJobIdBatches()) {
				// get jobs done by work manager thread
				resourceAdapter
						.getBootstrapCtx()
						.getWorkManager()
						.scheduleWork(new JobExecutionWork(resourceAdapter, jobIds, commandExecutor),
								WorkManager.INDEFINITE, null, retryWorkListener);
			}

			// if all jobs were executed
			millisToWait = jobExecutor.getWaitTimeInMillis();
			int jobsAcquired = acquiredJobs.getJobIdBatches().size();
			if (jobsAcquired < maxJobsPerAcquisition) {

				isJobAdded = false;

				// check if the next timer should fire before the normal sleep time is over
				Date duedate = new Date(ClockUtil.getCurrentTime().getTime() + millisToWait);
				List<TimerEntity> nextTimers = commandExecutor.execute(new GetUnlockedTimersByDuedateCmd(duedate,
						new Page(0, 1)));

				if (!nextTimers.isEmpty()) {
					long millisTillNextTimer = nextTimers.get(0).getDuedate().getTime()
							- ClockUtil.getCurrentTime().getTime();
					if (millisTillNextTimer < millisToWait) {
						millisToWait = millisTillNextTimer;
					}
				}

			} else {
				millisToWait = 0;
			}

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "exception during job acquisition: " + e.getMessage(), e);
			millisToWait *= waitIncreaseFactor;
			if (millisToWait > maxWait) {
				millisToWait = maxWait;
			} else if (millisToWait == 0) {
				millisToWait = jobExecutor.getWaitTimeInMillis();
			}
		}

		if ((millisToWait > 0) && (!isJobAdded)) {
			try {
				LOGGER.fine("job acquisition thread sleeping for " + millisToWait + " millis");
				synchronized (MONITOR) {
					if (!isInterrupted) {
						isWaiting.set(true);
						MONITOR.wait(millisToWait);
					}
				}
				LOGGER.fine("job acquisition thread woke up");
				isJobAdded = false;
			} catch (InterruptedException e) {
				LOGGER.fine("job acquisition wait interrupted");
			} finally {
				isWaiting.set(false);
			}
		}

		if (!isInterrupted) {
			try {
				LOGGER.info(jobExecutor.getName() + " job acquisition thread done. start next run.");
				resourceAdapter.getBootstrapCtx().getWorkManager()
						.scheduleWork(this, WorkManager.INDEFINITE, null, retryWorkListener);
			} catch (WorkException e) {
				LOGGER.log(Level.SEVERE, "exception during scheduleWork of job acquisition: " + e.getMessage(), e);
			}
		}
	}

	@Override
	public void release() {
	}

	public void stop() {
		LOGGER.finer("stop()");
		synchronized (MONITOR) {
			isInterrupted = true;
			if (isWaiting.compareAndSet(true, false)) {
				MONITOR.notifyAll();
			}
		}
	}

	public void jobWasAdded() {
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
			LOGGER.log(Level.FINE, "work rejected with code {0}. will retry.", new Object[] { event.getType() });
			try {
				resourceAdapter.getBootstrapCtx().getWorkManager()
						.scheduleWork(event.getWork(), WorkManager.INDEFINITE, null, this);
			} catch (WorkException e) {
				LOGGER.log(Level.SEVERE, "exception during scheduleWork of work "
						+ event.getWork().getClass().getName() + ": " + e.getMessage(), e);
			}
		}

		@Override
		public void workStarted(WorkEvent arg0) {
			// do nothing
		}

	}
}
