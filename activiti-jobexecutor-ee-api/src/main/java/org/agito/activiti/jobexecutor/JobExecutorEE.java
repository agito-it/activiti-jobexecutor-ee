package org.agito.activiti.jobexecutor;

import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistry;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job Executor for Enterprise Environments.
 * 
 * It uses a central registry as entry point for job acquisition/execution. This can be provided by a JCA adapter.
 * The registry (and its environment) is completely in charge for providing an appropriate acquisition/execution strategy.
 * 
 * A callback is used to notify the registry about new jobs.
 * 
 * @author agito
 * 
 */
public class JobExecutorEE extends JobExecutor {

	private final static Logger LOGGER = LoggerFactory.getLogger(JobExecutorEE.class);

	protected String acquisitionName;
	protected JobWasAddedCallback jobWasAddedCallback;
	protected final Object JOB_WAS_ADDED_MONITOR = new Object();

	@Override
	public void start() {
		setName();
		super.start();
	}

	@Override
	protected void startExecutingJobs() {
		JobExecutorRegistry registry = null;
		try {
			synchronized (JOB_WAS_ADDED_MONITOR) {
				registry = ((JobExecutorRegistryFactory) InitialContext.doLookup(JobExecutorRegistryFactory.JNDI))
						.getRegistry();
				registry.registerJobExecutor(this);
			}
		} catch (NamingException e) {
			throw new ActivitiException("Error during lookup of JobExecutorRegistryFactory", e);
		} catch (ResourceException e) {
			throw new ActivitiException("Error when registering on JobExecutorRegistry", e);
		} finally {
			if (registry != null)
				try {
					registry.close();
				} catch (ResourceException e) {
					throw new ActivitiException("Error when closing registry connection", e);
				}
		}
	}

	@Override
	protected void stopExecutingJobs() {
		JobExecutorRegistry registry = null;
		try {
			synchronized (JOB_WAS_ADDED_MONITOR) {
				registry = ((JobExecutorRegistryFactory) InitialContext.doLookup(JobExecutorRegistryFactory.JNDI))
						.getRegistry();
				registry.detachJobExecutor(this);
			}
		} catch (IllegalArgumentException e) {
			LOGGER.debug(
					"Error during lookup of JobExecutorRegistryFactory when stopping job executor. Server might be in shutdown phase.",
					e); // CPS-450 java.lang.IllegalArgumentException: JBAS011857: NamingStore ist Null
		} catch (NamingException e) {
			LOGGER.debug(
					"Error during lookup of JobExecutorRegistryFactory when stopping job executor. Server might be in shutdown phase.",
					e);
		} catch (ResourceException e) {
			throw new ActivitiException("Error when detaching from JobExecutorRegistry", e);
		} finally {
			if (registry != null)
				try {
					registry.close();
				} catch (ResourceException e) {
					throw new ActivitiException("Error when closing registry connection", e);
				}
		}
	}

	@Override
	protected void startJobAcquisitionThread() {
		// do nothing
	}

	@Override
	protected void stopJobAcquisitionThread() {
		// do nothing
	}

	@Override
	public void executeJobs(List<String> jobIds) {
		// do nothing
	}

	@Override
	public void jobWasAdded() {
		synchronized (JOB_WAS_ADDED_MONITOR) {
			if (jobWasAddedCallback != null && isActive)
				jobWasAddedCallback.jobWasAdded();
		}
	}

	public void setJobWasAddedCallback(JobWasAddedCallback callback) {
		this.jobWasAddedCallback = callback;
	}

	public void setAcquisitionName(String acquisitionName) {
		this.acquisitionName = acquisitionName;
	}

	public String getAcquisitionName() {
		return acquisitionName;
	}

	public void setName() {
		super.name = JobExecutorEE.class.getSimpleName() + "[" + commandExecutor.execute(new Command<String>() {
			@Override
			public String execute(CommandContext commandContext) {
				return Context.getProcessEngineConfiguration().getProcessEngineName();
			}
		}) + "]";
	}
}
