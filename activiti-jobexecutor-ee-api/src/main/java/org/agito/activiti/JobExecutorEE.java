package org.agito.activiti;

import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;

import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.agito.activiti.jobexecutor.api.JobExecutorInfo;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistry;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistryFactory;

public class JobExecutorEE extends JobExecutor {

	protected JobWasAddedCallback jobWasAddedCallback;
	protected final Object MONITOR = new Object();

	@Override
	protected void startExecutingJobs() {
		JobExecutorRegistry registry = null;
		try {
			registry = ((JobExecutorRegistryFactory) InitialContext.doLookup(JobExecutorRegistryFactory.JNDI))
					.getRegistry(new JobExecutorInfo("default"));
			registry.registerJobExecutor(this);
		} catch (NamingException e) {
			throw new RuntimeException("Error during lookup of JobExecutorRegistryFactory", e);
		} catch (ResourceException e) {
			throw new RuntimeException("Error when registering on JobExecutorRegistry", e);
		} finally {
			if (registry != null)
				try {
					registry.close();
				} catch (ResourceException e) {
					throw new RuntimeException(e);
				}
			synchronized (MONITOR) {
				jobWasAddedCallback = null;
			}
		}
	}

	@Override
	protected void stopExecutingJobs() {
		JobExecutorRegistry registry = null;
		try {
			registry = ((JobExecutorRegistryFactory) InitialContext.doLookup(JobExecutorRegistryFactory.JNDI))
					.getRegistry(new JobExecutorInfo("default"));
			registry.detachJobExecutor(this);
		} catch (NamingException e) {
			throw new RuntimeException("Error during lookup of JobExecutorRegistryFactory", e);
		} catch (ResourceException e) {
			throw new RuntimeException("Error when detaching from JobExecutorRegistry", e);
		} finally {
			if (registry != null)
				try {
					registry.close();
				} catch (ResourceException e) {
					throw new RuntimeException(e);
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
		synchronized (MONITOR) {
			if (jobWasAddedCallback != null)
				jobWasAddedCallback.jobWasAdded();
		}
	}

	public void registerJobWasAddedCallback(JobWasAddedCallback callback) {
		this.jobWasAddedCallback = callback;
	}

}
