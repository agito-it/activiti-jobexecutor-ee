package org.agito.activiti.jobexecutor.impl;

import java.lang.reflect.Method;

import javax.resource.ResourceException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;

import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.agito.activiti.jobexecutor.api.JobExecutorDispatcher;

public class JobExecutorDispatcherWork implements Work {

	private final MessageEndpointFactory messageEndpointFactory;
	private final Method dispatcherMethod;
	private final String jobId;
	private final CommandExecutor commandExecutor;

	public JobExecutorDispatcherWork(MessageEndpointFactory messageEndpointFactory, Method dispatcherMethod,
			String jobId, CommandExecutor commandExecutor) {
		this.messageEndpointFactory = messageEndpointFactory;
		this.dispatcherMethod = dispatcherMethod;
		this.jobId = jobId;
		this.commandExecutor = commandExecutor;
	}

	/* work */

	@Override
	public void run() {
		try {
			MessageEndpoint messageEndpoint = messageEndpointFactory.createEndpoint(null);
			try {
				messageEndpoint.beforeDelivery(dispatcherMethod);
				((JobExecutorDispatcher) messageEndpoint).dispatch(jobId, commandExecutor);
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("JobExecutorDispatcher has no dispatch functionality");
			} catch (ResourceException e) {
				throw new RuntimeException(e); // TODO
			} finally {
				if (messageEndpoint != null) {
					try {
						messageEndpoint.afterDelivery();
					} catch (ResourceException e) {
						throw new RuntimeException(e); // TODO
					}
					messageEndpoint.release();
				}
			}
		} catch (UnavailableException e) {
			throw new RuntimeException("JobExecutorDispatcher is not available.");
		}
	}

	@Override
	public void release() {
		// do nothing. synchronous > do not release mdb within tx.
	}

}
