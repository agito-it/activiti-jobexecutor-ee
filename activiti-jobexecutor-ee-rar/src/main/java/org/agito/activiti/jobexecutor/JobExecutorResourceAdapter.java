package org.agito.activiti.jobexecutor;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;

public class JobExecutorResourceAdapter implements ResourceAdapter {

	protected WorkManager workManager;

	/**
	 * default constructor
	 */
	public JobExecutorResourceAdapter() {

	}

	@Override
	public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
		this.workManager = ctx.getWorkManager();
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
	}

	/* unsupported operations */

	@Override
	public void endpointActivation(MessageEndpointFactory arg0, ActivationSpec arg1) throws ResourceException {
		throw new NotSupportedException("EndpointActivation not supported");
	}

	@Override
	public void endpointDeactivation(MessageEndpointFactory arg0, ActivationSpec arg1) {
	}

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

}
