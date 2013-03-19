package org.agito.activiti.jobexecutor.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.activiti.engine.impl.cmd.ExecuteJobsCmd;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.agito.activiti.jobexecutor.api.JobExecutorDispatcher;

@MessageDriven(messageListenerInterface = JobExecutorDispatcher.class)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class JobExecutorDispatcherImpl implements JobExecutorDispatcher {

	private final static Logger LOGGER = Logger.getLogger(JobExecutorDispatcherImpl.class.getName());

	public JobExecutorDispatcherImpl() {
	}

	@Override
	public void dispatch(String jobId, CommandExecutor commandExecutor) {
		LOGGER.log(Level.FINER, "dispatch(jobId={0}, commandExecutor)", new Object[] { jobId });

		commandExecutor.execute(new ExecuteJobsCmd(jobId));

	}

}
