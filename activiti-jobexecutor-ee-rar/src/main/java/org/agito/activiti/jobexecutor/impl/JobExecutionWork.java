package org.agito.activiti.jobexecutor.impl;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.resource.spi.work.Work;

import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.jobexecutor.JobExecutorContext;
import org.agito.activiti.jobexecutor.JobExecutorResourceAdapter;

public class JobExecutionWork implements Work {

	private final static Logger LOGGER = Logger.getLogger(JobExecutionWork.class.getName());

	protected final JobExecutorResourceAdapter resourceAdapter;
	protected final List<String> jobIds;
	protected final CommandExecutor commandExecutor;

	public JobExecutionWork(JobExecutorResourceAdapter resourceAdapter, List<String> jobIds,
			CommandExecutor commandExecutor) {
		this.resourceAdapter = resourceAdapter;
		this.jobIds = jobIds;
		this.commandExecutor = commandExecutor;
	}

	@Override
	public void run() {

		final JobExecutorContext jobExecutorContext = new JobExecutorContext();
		final List<String> currentProcessorJobQueue = jobExecutorContext.getCurrentProcessorJobQueue();

		currentProcessorJobQueue.addAll(jobIds);

		Context.setJobExecutorContext(jobExecutorContext);
		try {
			while (!currentProcessorJobQueue.isEmpty()) {
				String nextJobId = currentProcessorJobQueue.remove(0);
				LOGGER.log(Level.FINE, "Dispatching job {0}.", new Object[] { nextJobId });
				resourceAdapter.getJobExecutorActivation().dispatch(nextJobId, commandExecutor);
			}
		} finally {
			Context.removeJobExecutorContext();
		}
	}

	@Override
	public void release() {
	}

}
