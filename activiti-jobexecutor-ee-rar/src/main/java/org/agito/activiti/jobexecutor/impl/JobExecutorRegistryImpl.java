package org.agito.activiti.jobexecutor.impl;

import java.util.logging.Logger;

import javax.resource.ResourceException;

import org.agito.activiti.JobExecutorEE;
import org.agito.activiti.jobexecutor.api.JobExecutorInfo;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistry;

public class JobExecutorRegistryImpl implements JobExecutorRegistry {

	private final static Logger LOGGER = Logger.getLogger(JobExecutorRegistryImpl.class.getName());

	final private JobExecutorInfo properties;
	final private JobExecutorManagedConnection managedConnection;

	public JobExecutorRegistryImpl(JobExecutorManagedConnection managedConnection, JobExecutorInfo properties) {
		LOGGER.finer("JobExecutorRegistryImpl(JobExecutorManagedConnection, JobExecutorInfo)");
		this.managedConnection = managedConnection;
		this.properties = properties;
	}

	@Override
	public void close() throws ResourceException {
		LOGGER.finer("close()");
		if (this.managedConnection != null) {
			this.managedConnection.closeHandle(this);
		}
	}

	@Override
	public void registerJobExecutor(JobExecutorEE jobExecutorEE) throws ResourceException {
		LOGGER.finer("registerJobExecutor(jobExecutorEE)");
		managedConnection.getJobExecutorManagedConnectionFactory().getResourceAdapter()
				.registerJobExecutor(jobExecutorEE);
	}

	@Override
	public void detachJobExecutor(JobExecutorEE jobExecutorEE) throws ResourceException {
		LOGGER.finer("detachJobExecutor(jobExecutorEE)");
		managedConnection.getJobExecutorManagedConnectionFactory().getResourceAdapter()
				.detachJobExecutor(jobExecutorEE);
	}

}
