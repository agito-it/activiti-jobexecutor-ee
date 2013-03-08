package org.agito.activiti.jobexecutor.api;

import javax.resource.ResourceException;

public interface JobExecutorRegistryFactory {

	public final static String JNDI = "env/ActivitiJobExecutor";

	public JobExecutorRegistry getRegistry(JobExecutorInfo properties) throws ResourceException;

}
