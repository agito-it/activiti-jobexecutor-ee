package org.agito.activiti.jobexecutor;

import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistry;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistryFactory;

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

	protected String acquisitionName;
	protected JobWasAddedCallback jobWasAddedCallback;
	protected final Object JOB_WAS_ADDED_MONITOR = new Object();

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
		} catch (NamingException e) {
			throw new ActivitiException("Error during lookup of JobExecutorRegistryFactory", e);
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

}
