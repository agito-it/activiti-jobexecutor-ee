package org.agito.activiti.jobexecutor.api;

import javax.resource.ResourceException;

public interface JobExecutorRegistry {

	public void close() throws ResourceException;

}
