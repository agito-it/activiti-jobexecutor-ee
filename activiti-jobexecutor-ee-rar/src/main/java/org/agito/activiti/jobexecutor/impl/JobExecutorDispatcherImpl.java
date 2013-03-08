package org.agito.activiti.jobexecutor.impl;

import javax.ejb.MessageDriven;

import org.activiti.engine.impl.cmd.ExecuteJobsCmd;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.agito.activiti.jobexecutor.api.JobExecutorDispatcher;

@MessageDriven(messageListenerInterface = JobExecutorDispatcher.class)
public class JobExecutorDispatcherImpl implements JobExecutorDispatcher {

	@Override
	public void dispatch(String jobId, CommandExecutor commandExecutor) {

		commandExecutor.execute(new ExecuteJobsCmd(jobId));

	}

}
