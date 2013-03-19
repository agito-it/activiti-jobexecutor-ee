package org.agito.activiti.jobexecutor.api;

import org.activiti.engine.impl.interceptor.CommandExecutor;

/**
 * Dispatcher interface for a message driven bean that is in charge for the actual job execution.
 * 
 * @author agito
 * 
 */
public interface JobExecutorDispatcher {

	public void dispatch(String jobId, CommandExecutor commandExecutor);

}
