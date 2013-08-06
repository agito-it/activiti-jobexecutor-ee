package org.agito.activiti.jobexecutor.ra.impl.work;

import java.util.List;

import javax.resource.spi.work.Work;

import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.jobexecutor.JobExecutorContext;
import org.agito.activiti.jobexecutor.ra.JobExecutorResourceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobExecutionWork implements Work {

	private final static Logger LOGGER = LoggerFactory.getLogger(JobExecutionWork.class);

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
				LOGGER.debug("Dispatching job {}.", new Object[] { nextJobId });
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
