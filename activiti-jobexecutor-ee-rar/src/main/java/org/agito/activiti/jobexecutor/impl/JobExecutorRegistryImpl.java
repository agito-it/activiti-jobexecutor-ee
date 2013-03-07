package org.agito.activiti.jobexecutor.impl;

import javax.resource.ResourceException;

import org.agito.activiti.jobexecutor.api.JobExecutorRegistry;
import org.agito.activiti.jobexecutor.api.JobExecutorInfo;

public class JobExecutorRegistryImpl implements JobExecutorRegistry {

	final private JobExecutorInfo properties;
	final private JobExecutorManagedConnection managedConnection;

	public JobExecutorRegistryImpl(JobExecutorManagedConnection managedConnection, JobExecutorInfo properties) {
		this.managedConnection = managedConnection;
		this.properties = properties;
	}

	@Override
	public void close() throws ResourceException {
		if (this.managedConnection != null) {
			this.managedConnection.closeHandle(this);
		}
	}

}
