package org.agito.activiti.jobexecutor.api;

import javax.resource.ResourceException;

import org.agito.activiti.jobexecutor.JobExecutorEE;

/**
 * Interface of the central registry.
 * 
 * @author agito
 * 
 */
public interface JobExecutorRegistry {

	public void registerJobExecutor(JobExecutorEE jobExecutorEE) throws ResourceException;

	public void detachJobExecutor(JobExecutorEE jobExecutorEE) throws ResourceException;

	public void close() throws ResourceException;

}
