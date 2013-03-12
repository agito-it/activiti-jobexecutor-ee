package org.agito.activiti.jobexecutor.impl;

import java.util.logging.Logger;

import javax.ejb.MessageDriven;

import org.activiti.engine.impl.cmd.ExecuteJobsCmd;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.agito.activiti.jobexecutor.api.JobExecutorDispatcher;

@MessageDriven(messageListenerInterface = JobExecutorDispatcher.class)
public class JobExecutorDispatcherImpl implements JobExecutorDispatcher {

	private final static Logger LOGGER = Logger.getLogger(JobExecutorDispatcherImpl.class.getName());

	public JobExecutorDispatcherImpl() {
		LOGGER.finer("JobExecutorDispatcherImpl()");
	}

	@Override
	public void dispatch(String jobId, CommandExecutor commandExecutor) {
		LOGGER.finer("dispatch(jobId, commandExecutor)");

		commandExecutor.execute(new ExecuteJobsCmd(jobId));

	}

}
