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

import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.jobexecutor.AcquiredJobs;
import org.activiti.engine.impl.jobexecutor.GetUnlockedTimersByDuedateCmd;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.activiti.engine.impl.util.ClockUtil;
import org.agito.activiti.JobExecutorEE;
import org.agito.activiti.jobexecutor.api.JobExecutorDispatcher;

public class JobAcquisitionWork implements Work {

	private final static Logger LOGGER = Logger.getLogger(JobAcquisitionWork.class.getName());

	protected final JobExecutorEE jobExecutor;
	protected final JobExecutorDispatcher jobDispatcher;
	protected final WorkManager workManager;

	protected volatile boolean isInterrupted = false;
	protected final Object MONITOR = new Object();

	protected final AtomicBoolean isWaiting = new AtomicBoolean(false);

	protected long millisToWait = 0;
	protected float waitIncreaseFactor = 2;
	protected long maxWait = 60 * 1000;

	public JobAcquisitionWork(JobExecutorEE jobExecutor, JobExecutorDispatcher jobDispatcher, WorkManager workManager) {
		this.jobExecutor = jobExecutor;
		this.jobDispatcher = jobDispatcher;
		this.workManager = workManager;
	}

	@Override
	public void run() {

		if (!isInterrupted) {
			if (LOGGER.isLoggable(Level.INFO)) {
				LOGGER.info(jobExecutor.getName() + " acquiring jobs.");
			}
		} else {
			if (LOGGER.isLoggable(Level.INFO)) {
				LOGGER.info(jobExecutor.getName() + " job executor not active anymore. Cancelling job acquisition.");
			}
			return;
		}

		final CommandExecutor commandExecutor = jobExecutor.getCommandExecutor();

		int maxJobsPerAcquisition = jobExecutor.getMaxJobsPerAcquisition();

		try {
			AcquiredJobs acquiredJobs = commandExecutor.execute(jobExecutor.getAcquireJobsCmd());

			jobExecutor.getRejectedJobsHandler();
			for (List<String> jobIds : acquiredJobs.getJobIdBatches()) {
				for (String jobId : jobIds) {
					jobDispatcher.dispatch(jobId, commandExecutor);
				}
			}

			// if all jobs were executed
			millisToWait = jobExecutor.getWaitTimeInMillis();
			int jobsAcquired = acquiredJobs.getJobIdBatches().size();
			if (jobsAcquired < maxJobsPerAcquisition) {

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

		} catch (ActivitiOptimisticLockingException optimisticLockingException) {
			// See http://jira.codehaus.org/browse/ACT-1390
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.fine("Optimistic locking exception during job acquisition. If you have multiple job executors running against the same database, "
						+ "this exception means that this thread tried to acquire a job, which already was acquired by another job executor acquisition thread."
						+ "This is expected behavior in a clustered environment. "
						+ "You can ignore this message if you indeed have multiple job executor acquisition threads running against the same database. "
						+ "Exception message: " + optimisticLockingException.getMessage());
			}
		} catch (Exception e) {
			if (LOGGER.isLoggable(Level.SEVERE)) {
				LOGGER.log(Level.SEVERE, "exception during job acquisition: " + e.getMessage(), e);
			}
			millisToWait *= waitIncreaseFactor;
			if (millisToWait > maxWait) {
				millisToWait = maxWait;
			} else if (millisToWait == 0) {
				millisToWait = jobExecutor.getWaitTimeInMillis();
			}
		}

		if ((millisToWait > 0)) {
			try {
				if (LOGGER.isLoggable(Level.FINE)) {
					LOGGER.fine("job acquisition thread sleeping for " + millisToWait + " millis");
				}

				synchronized (MONITOR) {
					if (!isInterrupted) {
						isWaiting.set(true);
						MONITOR.wait(millisToWait);
					}

				}

				if (LOGGER.isLoggable(Level.FINE)) {
					LOGGER.fine("job acquisition thread woke up");
				}
			} catch (InterruptedException e) {
				if (LOGGER.isLoggable(Level.FINE)) {
					LOGGER.fine("job acquisition wait interrupted");
				}
			} finally {
				isWaiting.set(false);
			}
		}

		if (!isInterrupted) {
			if (LOGGER.isLoggable(Level.INFO)) {
				LOGGER.info(jobExecutor.getName() + " job acquisition done. scheduling next run.");
			}

			try {
				workManager.scheduleWork(this, WorkManager.INDEFINITE, null, new RetryListener(workManager));
			} catch (WorkException e) {
				if (LOGGER.isLoggable(Level.SEVERE)) {
					LOGGER.log(Level.SEVERE, "exception during scheduleWork of job acquisition: " + e.getMessage(), e);
				}
			}
		}

	}

	@Override
	public void release() {
		LOGGER.finer("release()");
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

	private static final class RetryListener implements WorkListener {

		protected final WorkManager workManager;

		public RetryListener(WorkManager workManager) {
			this.workManager = workManager;
		}

		@Override
		public void workAccepted(WorkEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void workCompleted(WorkEvent arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void workRejected(WorkEvent event) {
			if (LOGGER.isLoggable(Level.FINE)) {
				LOGGER.log(Level.FINE, "work rejected with code {0}. will retry.", new Object[] { event.getType() });
			}

			try {
				workManager.scheduleWork(event.getWork(), WorkManager.INDEFINITE, null, this);
			} catch (WorkException e) {
				if (LOGGER.isLoggable(Level.SEVERE)) {
					LOGGER.log(Level.SEVERE, "exception during scheduleWork of job acquisition: " + e.getMessage(), e);
				}
			}
		}

		@Override
		public void workStarted(WorkEvent arg0) {
			// TODO Auto-generated method stub

		}

	}
}
