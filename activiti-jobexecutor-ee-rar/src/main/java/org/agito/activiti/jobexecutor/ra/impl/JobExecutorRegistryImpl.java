package org.agito.activiti.jobexecutor.ra.impl;

import java.util.logging.Logger;

import javax.resource.ResourceException;

import org.agito.activiti.jobexecutor.JobExecutorEE;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistry;
import org.agito.activiti.jobexecutor.ra.impl.work.JobAcquisitionWork;

public class JobExecutorRegistryImpl implements JobExecutorRegistry {

	private final static Logger LOGGER = Logger.getLogger(JobExecutorRegistryImpl.class.getName());

	final private JobExecutorManagedConnection managedConnection;

	public JobExecutorRegistryImpl(JobExecutorManagedConnection managedConnection) {
		LOGGER.finer("JobExecutorRegistryImpl(JobExecutorManagedConnection, JobExecutorInfo)");
		this.managedConnection = managedConnection;
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
		LOGGER.finer("detachJobExecutor(jobExecutorEE)");
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
