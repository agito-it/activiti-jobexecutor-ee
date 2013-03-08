package org.agito.activiti.jobexecutor.impl;

import java.lang.reflect.Method;
import java.util.concurrent.RejectedExecutionException;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkRejectedException;

import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.agito.activiti.jobexecutor.JobExecutorResourceAdapter;
import org.agito.activiti.jobexecutor.api.JobExecutorDispatcher;

public class JobExecutorActivation implements JobExecutorDispatcher, ActivationSpec {

	private JobExecutorResourceAdapter resourceAdapter;
	private MessageEndpointFactory messageEndpointFactory;

	/* static initialization of the dispatcher method */
	static final Method DISPATCH;
	static {
		try {
			DISPATCH = JobExecutorDispatcher.class.getMethod("dispatch", new Class[] { String.class,
					CommandExecutor.class });
		} catch (Throwable t) {
			throw new ExceptionInInitializerError(t);
		}
	}

	@Override
	public void dispatch(String jobId, CommandExecutor commandExecutor) {
		try {
			// doWork(..) is synchronous >> blocks until the Work instance completes
			resourceAdapter.getBootstrapCtx().getWorkManager()
					.doWork(new JobExecutorDispatcherWork(messageEndpointFactory, DISPATCH, jobId, commandExecutor));
		} catch (WorkException e) {
			if (e instanceof WorkRejectedException) {
				throw new RejectedExecutionException(e);
			} else {
				throw new RuntimeException(e);
				// TODO maybe unwrap WorkCompletedException
			}

		}
	}

	public void cleanup() {
		// do nothing. synchronous > do not release mdb within tx.
	}

	/* activation spec */

	@Override
	public ResourceAdapter getResourceAdapter() {
		return resourceAdapter;
	}

	@Override
	public void setResourceAdapter(ResourceAdapter resourceAdapter) throws ResourceException {
		if (!JobExecutorResourceAdapter.class.isAssignableFrom(resourceAdapter.getClass()))
			throw new ResourceException("Invalid resource adapter type");
		this.resourceAdapter = (JobExecutorResourceAdapter) resourceAdapter;
	}

	public void setMessageEndpointFactory(MessageEndpointFactory messageEndpointFactory) {
		this.messageEndpointFactory = messageEndpointFactory;
	}

	/* contract */

	@Override
	public void validate() throws InvalidPropertyException {
		// ignore
	}

}
