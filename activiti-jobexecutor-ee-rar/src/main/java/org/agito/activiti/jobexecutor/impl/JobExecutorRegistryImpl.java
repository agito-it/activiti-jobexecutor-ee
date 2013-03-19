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
		if (properties == null) {
			managedConnection.getJobExecutorManagedConnectionFactory().getResourceAdapter().getDefaultJobAcquistion()
					.registerJobExecutor(jobExecutorEE);
		} else {
			JobAcquisitionWork jobAcquisition = managedConnection.getJobExecutorManagedConnectionFactory()
					.getResourceAdapter().getJobAcquisitionMap().get(properties.getJobExecutorId());
			if (jobAcquisition == null)
				throw new ResourceException("Job acquisition " + properties.getJobExecutorId() + " does not exist.");

			jobAcquisition.registerJobExecutor(jobExecutorEE);
		}
	}

	@Override
	public void detachJobExecutor(JobExecutorEE jobExecutorEE) throws ResourceException {
		LOGGER.finer("detachJobExecutor(jobExecutorEE)");
		if (properties == null) {
			managedConnection.getJobExecutorManagedConnectionFactory().getResourceAdapter().getDefaultJobAcquistion()
					.detachJobExecutor(jobExecutorEE);
		} else {
			JobAcquisitionWork jobAcquisition = managedConnection.getJobExecutorManagedConnectionFactory()
					.getResourceAdapter().getJobAcquisitionMap().get(properties.getJobExecutorId());
			if (jobAcquisition == null)
				throw new ResourceException("Job acquisition " + properties.getJobExecutorId() + " does not exist.");

			jobAcquisition.detachJobExecutor(jobExecutorEE);
		}
	}

}
