package org.agito.activiti.jobexecutor.impl;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;

import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.agito.activiti.jobexecutor.JobExecutorResourceAdapter;
import org.agito.activiti.jobexecutor.api.JobExecutorDispatcher;

public class JobExecutorActivation implements JobExecutorDispatcher, ActivationSpec {

	private final static Logger LOGGER = Logger.getLogger(JobExecutorActivation.class.getName());

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
			MessageEndpoint messageEndpoint = messageEndpointFactory.createEndpoint(null);
			try {
				messageEndpoint.beforeDelivery(DISPATCH);
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

	public void cleanup() {
		// do nothing. synchronous > do not release mdb within tx.
	}

	/* activation spec */

	@Override
	public ResourceAdapter getResourceAdapter() {
		LOGGER.finer("call getResourceAdapter()");
		return resourceAdapter;
	}

	@Override
	public void setResourceAdapter(ResourceAdapter resourceAdapter) throws ResourceException {
		LOGGER.finer("call setResourceAdapter(resourceAdapter)");
		if (!JobExecutorResourceAdapter.class.isAssignableFrom(resourceAdapter.getClass()))
			throw new ResourceException("Invalid resource adapter type");
		this.resourceAdapter = (JobExecutorResourceAdapter) resourceAdapter;
	}

	public void setMessageEndpointFactory(MessageEndpointFactory messageEndpointFactory) {
		LOGGER.finer("call setMessageEndpointFactory()");
		this.messageEndpointFactory = messageEndpointFactory;
	}

	/* contract */

	@Override
	public void validate() throws InvalidPropertyException {
		LOGGER.finer("call validate()");
		// ignore
	}

}
