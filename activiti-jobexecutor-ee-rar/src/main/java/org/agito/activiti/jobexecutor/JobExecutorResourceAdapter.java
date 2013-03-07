package org.agito.activiti.jobexecutor;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.agito.activiti.jobexecutor.impl.JobExecutorActivation;

public class JobExecutorResourceAdapter implements ResourceAdapter {

	private BootstrapContext bootstrapCtx;
	private JobExecutorActivation jobExecutorActivation;

	/**
	 * default constructor
	 */
	public JobExecutorResourceAdapter() {

	}

	@Override
	public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
		this.bootstrapCtx = ctx;
	}

	@Override
	public void stop() {
		this.bootstrapCtx = null;
	}

	@Override
	public void endpointActivation(MessageEndpointFactory mef, ActivationSpec activationSpec) throws ResourceException {
		if (!JobExecutorActivation.class.isAssignableFrom(activationSpec.getClass()))
			throw new ResourceException("Invalid activation spec type");
		jobExecutorActivation = (JobExecutorActivation) activationSpec;
		jobExecutorActivation.validate(); // jca contract
		jobExecutorActivation.setMessageEndpointFactory(mef);
	}

	@Override
	public void endpointDeactivation(MessageEndpointFactory mef, ActivationSpec activationSpec) {
		jobExecutorActivation.cleanup();
	}

	/* unsupported operations */

	@Override
	public XAResource[] getXAResources(ActivationSpec[] arg0) throws ResourceException {
		throw new NotSupportedException("XAResources not supported");
	}

	/* equals / hashCode */

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	/* getter */

	public BootstrapContext getBootstrapCtx() {
		return bootstrapCtx;
	}

	public JobExecutorActivation getJobExecutorActivation() {
		return jobExecutorActivation;
	}

}
