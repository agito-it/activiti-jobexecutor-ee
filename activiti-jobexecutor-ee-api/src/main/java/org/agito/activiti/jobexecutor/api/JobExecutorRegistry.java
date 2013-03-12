package org.agito.activiti.jobexecutor.api;

import javax.resource.ResourceException;

import org.agito.activiti.JobExecutorEE;

public interface JobExecutorRegistry {

	public void registerJobExecutor(JobExecutorEE jobExecutorEE) throws ResourceException;

	public void detachJobExecutor(JobExecutorEE jobExecutorEE) throws ResourceException;

	public void close() throws ResourceException;

}
