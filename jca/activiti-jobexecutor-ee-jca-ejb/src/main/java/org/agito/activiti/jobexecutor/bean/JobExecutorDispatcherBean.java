package org.agito.activiti.jobexecutor.bean;

import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.activiti.engine.impl.cmd.ExecuteJobsCmd;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.agito.activiti.jobexecutor.api.JobExecutorDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@MessageDriven(messageListenerInterface = JobExecutorDispatcher.class)
public class JobExecutorDispatcherBean implements JobExecutorDispatcher {

	private final static Logger LOGGER = LoggerFactory.getLogger(JobExecutorDispatcherBean.class);

	@Override
	public void dispatch(String jobId, CommandExecutor commandExecutor) {
		LOGGER.trace("dispatch(jobId={}, commandExecutor)", new Object[] { jobId });
		commandExecutor.execute(new ExecuteJobsCmd(jobId));
	}

}
