package org.agito.activiti.jobexecutor.api;

import javax.resource.ResourceException;

public interface JobExecutorRegistryFactory {

	public JobExecutorRegistry getRegistry(JobExecutorInfo properties) throws ResourceException;

}
