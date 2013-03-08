package org.agito.activiti;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;

import org.activiti.engine.impl.jobexecutor.JobExecutor;
import org.agito.activiti.jobexecutor.api.JobExecutorDispatcher;
import org.agito.activiti.jobexecutor.api.JobExecutorInfo;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistryFactory;

public class JobExecutorEE extends JobExecutor {

	private JobExecutorDispatcher dispatcher;

	@Override
	protected void startExecutingJobs() {
		try {
			((JobExecutorRegistryFactory) InitialContext.doLookup(JobExecutorRegistryFactory.JNDI)).getRegistry(
					new JobExecutorInfo("default")).registerJobExecutor(this);
		} catch (NamingException e) {
			throw new RuntimeException(e);
		} catch (ResourceException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void stopExecutingJobs() {
		try {
			((JobExecutorRegistryFactory) InitialContext.doLookup(JobExecutorRegistryFactory.JNDI)).getRegistry(
					new JobExecutorInfo("default")).detachJobExecutor(this);
		} catch (NamingException e) {
			throw new RuntimeException(e);
		} catch (ResourceException e) {
			throw new RuntimeException(e);
		}
	}

	public void setDispatcher(JobExecutorDispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}

	@Override
	protected void executeJobs(List<String> jobIds) {
		try {
			for (String jobId : jobIds)
				dispatcher.dispatch(jobId, commandExecutor);
		} catch (RejectedExecutionException e) {
			rejectedJobsHandler.jobsRejected(this, jobIds);
		}

	}

}
