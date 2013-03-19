package org.agito.activiti.jobexecutor.bean;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.activiti.engine.impl.cmd.ExecuteJobsCmd;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.agito.activiti.jobexecutor.api.JobExecutorDispatcher;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@MessageDriven(messageListenerInterface = JobExecutorDispatcher.class)
public class JobExecutorDispatcherBean implements JobExecutorDispatcher {

	private final static Logger LOGGER = Logger.getLogger(JobExecutorDispatcherBean.class.getName());

	@Override
	public void dispatch(String jobId, CommandExecutor commandExecutor) {
		LOGGER.log(Level.FINER, "dispatch(jobId={0}, commandExecutor)", new Object[] { jobId });
		commandExecutor.execute(new ExecuteJobsCmd(jobId));
	}

}
