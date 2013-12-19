package org.agito.activiti.jobexecutor.ra.impl;

import javax.resource.ResourceException;

import org.agito.activiti.jobexecutor.JobExecutorEE;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistry;
import org.agito.activiti.jobexecutor.ra.impl.work.JobAcquisitionWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobExecutorRegistryImpl implements JobExecutorRegistry {

	private final static Logger LOGGER = LoggerFactory.getLogger(JobExecutorRegistryImpl.class);

	final private JobExecutorManagedConnection managedConnection;

	public JobExecutorRegistryImpl(JobExecutorManagedConnection managedConnection) {
		LOGGER.trace("JobExecutorRegistryImpl(JobExecutorManagedConnection, JobExecutorInfo)");
		this.managedConnection = managedConnection;
	}

	@Override
	public void close() throws ResourceException {
		LOGGER.trace("close()");
		if (this.managedConnection != null) {
			this.managedConnection.closeHandle(this);
		}
	}

	@Override
	public void registerJobExecutor(JobExecutorEE jobExecutorEE) throws ResourceException {
		LOGGER.trace("registerJobExecutor(jobExecutorEE)");

		// block registration in case the job executor dispatcher is not available
		if (null == managedConnection.getJobExecutorManagedConnectionFactory().getResourceAdapter()
				.getJobExecutorActivation()) {
			throw new ResourceException(
					"JobExecutorDispatcher is not ready yet. Ensure that the Job Executor Message Driven Bean is deployed and mapped to the Job Executor Resource Adapter.");
		}

		if (jobExecutorEE.getAcquisitionName() == null) {
			managedConnection.getJobExecutorManagedConnectionFactory().getResourceAdapter().getDefaultJobAcquistion()
					.registerJobExecutor(jobExecutorEE);
		} else {
			JobAcquisitionWork jobAcquisition = managedConnection.getJobExecutorManagedConnectionFactory()
					.getResourceAdapter().getJobAcquisitionMap().get(jobExecutorEE.getAcquisitionName());
			if (jobAcquisition == null)
				throw new ResourceException("Job acquisition " + jobExecutorEE.getAcquisitionName()
						+ " does not exist.");

			jobAcquisition.registerJobExecutor(jobExecutorEE);
		}
	}

	@Override
	public void detachJobExecutor(JobExecutorEE jobExecutorEE) throws ResourceException {
		LOGGER.trace("detachJobExecutor(jobExecutorEE)");
		if (jobExecutorEE.getAcquisitionName() == null) {
			managedConnection.getJobExecutorManagedConnectionFactory().getResourceAdapter().getDefaultJobAcquistion()
					.detachJobExecutor(jobExecutorEE);
		} else {
			JobAcquisitionWork jobAcquisition = managedConnection.getJobExecutorManagedConnectionFactory()
					.getResourceAdapter().getJobAcquisitionMap().get(jobExecutorEE.getAcquisitionName());
			if (jobAcquisition == null)
				throw new ResourceException("Job acquisition " + jobExecutorEE.getAcquisitionName()
						+ " does not exist.");

			jobAcquisition.detachJobExecutor(jobExecutorEE);
		}
	}

}
