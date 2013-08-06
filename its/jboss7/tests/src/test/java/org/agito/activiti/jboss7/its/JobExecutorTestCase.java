package org.agito.activiti.jboss7.its;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.ProcessEngineImpl;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.activiti.engine.impl.persistence.entity.MessageEntity;
import org.activiti.engine.impl.persistence.entity.TimerEntity;
import org.agito.activiti.jboss7.engine.impl.JobExecutorProcessEngineConfiguration;

public abstract class JobExecutorTestCase extends AbstractContainerTest {

	public final static String JOB_ACQUISITION_1 = "ONE";
	public final static String JOB_ACQUISITION_2 = "TWO";

	protected Map<String, ProcessEngineConfigurationImpl> processEngineConfigurations;
	protected Map<String, ProcessEngineImpl> processEngines;

	protected TweetHandler tweetHandler = new TweetHandler();

	public void setUp() throws Exception {
		processEngineConfigurations = new HashMap<String, ProcessEngineConfigurationImpl>();
		processEngines = new HashMap<String, ProcessEngineImpl>();
		for (String processEngineName : configureProcessEngineNames()) {
			ProcessEngineConfigurationImpl processEngineConfiguration = getProcessEngineConfigurationImpl(processEngineName);
			ProcessEngineImpl processEngine = (ProcessEngineImpl) processEngineConfiguration.buildProcessEngine();

			processEngineConfigurations.put(processEngineName, processEngineConfiguration);
			processEngines.put(processEngineName, processEngine);

			processEngineConfiguration.getJobHandlers().put(tweetHandler.getType(), tweetHandler);
		}

	}

	public void tearDown() throws Exception {
		for (Entry<String, ProcessEngineConfigurationImpl> e : processEngineConfigurations.entrySet()) {
			ProcessEngineConfigurationImpl processEngineConfiguration = e.getValue();
			ProcessEngineImpl processEngine = processEngines.get(e.getKey());

			if (processEngineConfiguration.getJobHandlers() != null)
				processEngineConfiguration.getJobHandlers().remove(tweetHandler.getType());

			if (processEngine != null)
				processEngine.close();
		}

	}

	protected MessageEntity createTweetMessage(String msg) {
		MessageEntity message = new MessageEntity();
		message.setJobHandlerType("tweet");
		message.setJobHandlerConfiguration(msg);
		return message;
	}

	protected TimerEntity createTweetTimer(String msg, Date duedate) {
		TimerEntity timer = new TimerEntity();
		timer.setJobHandlerType("tweet");
		timer.setJobHandlerConfiguration(msg);
		timer.setDuedate(duedate);
		return timer;
	}

	public void waitForJobExecutorToProcessAllJobs(long maxMillisToWait, long intervalMillis) {

		for (ProcessEngineConfigurationImpl processEngineConfiguration : processEngineConfigurations.values()) {
			JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
			jobExecutor.start();
		}

		try {
			Timer timer = new Timer();
			InteruptTask task = new InteruptTask(Thread.currentThread());
			timer.schedule(task, maxMillisToWait);
			boolean areJobsAvailable = true;
			try {
				while (areJobsAvailable && !task.isTimeLimitExceeded()) {
					Thread.sleep(intervalMillis);
					areJobsAvailable = areJobsAvailable();
				}
			} catch (InterruptedException e) {
			} finally {
				timer.cancel();
			}
			if (areJobsAvailable) {
				throw new RuntimeException("time limit of " + maxMillisToWait + " was exceeded");
			}

		} finally {
			for (ProcessEngineConfigurationImpl processEngineConfiguration : processEngineConfigurations.values()) {
				JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
				jobExecutor.shutdown();
			}
		}
	}

	public void waitForJobExecutorOnCondition(long maxMillisToWait, long intervalMillis, Callable<Boolean> condition) {
		for (ProcessEngineConfigurationImpl processEngineConfiguration : processEngineConfigurations.values()) {
			JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
			jobExecutor.start();
		}

		try {
			Timer timer = new Timer();
			InteruptTask task = new InteruptTask(Thread.currentThread());
			timer.schedule(task, maxMillisToWait);
			boolean conditionIsViolated = true;
			try {
				while (conditionIsViolated) {
					Thread.sleep(intervalMillis);
					conditionIsViolated = !condition.call();
				}
			} catch (InterruptedException e) {
			} catch (Exception e) {
				throw new ActivitiException("Exception while waiting on condition: " + e.getMessage(), e);
			} finally {
				timer.cancel();
			}
			if (conditionIsViolated) {
				throw new ActivitiException("time limit of " + maxMillisToWait + " was exceeded");
			}

		} finally {
			for (ProcessEngineConfigurationImpl processEngineConfiguration : processEngineConfigurations.values()) {
				JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
				jobExecutor.shutdown();
			}
		}
	}

	public boolean areJobsAvailable() {
		for (ProcessEngineConfigurationImpl processEngineConfiguration : processEngineConfigurations.values()) {
			if (!processEngineConfiguration.getManagementService().createJobQuery().executable().list().isEmpty()) {
				return true;
			}
		}
		return false;
	}

	private static class InteruptTask extends TimerTask {
		protected boolean timeLimitExceeded = false;
		protected Thread thread;

		public InteruptTask(Thread thread) {
			this.thread = thread;
		}

		public boolean isTimeLimitExceeded() {
			return timeLimitExceeded;
		}

		public void run() {
			timeLimitExceeded = true;
			thread.interrupt();
		}
	}

	public JobExecutorProcessEngineConfiguration getProcessEngineConfigurationImpl(String processEngineName) {
		JobExecutorProcessEngineConfiguration ret = super.getProcessEngineConfigurationImpl(processEngineName);
		ret.setJobAcquisitionName(this.configureJobAcquisitionForProcessEngine(processEngineName));
		return ret;
	}

	public abstract String configureJobAcquisitionForProcessEngine(String processEngine);

	public abstract String[] configureProcessEngineNames();

}
