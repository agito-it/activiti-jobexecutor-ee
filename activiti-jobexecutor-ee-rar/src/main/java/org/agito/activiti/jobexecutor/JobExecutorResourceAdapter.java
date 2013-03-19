package org.agito.activiti.jobexecutor;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.agito.activiti.JobExecutorEE;
import org.agito.activiti.jobexecutor.impl.JobAcquisitionWork;
import org.agito.activiti.jobexecutor.impl.JobExecutorActivation;
import org.agito.activiti.jobexecutor.impl.config.JobConfigurationAccessorImpl;
import org.agito.activiti.jobexecutor.impl.config.JobConfigurationSectionImpl;

public class JobExecutorResourceAdapter implements ResourceAdapter {

	private final static Logger LOGGER = Logger.getLogger(JobExecutorResourceAdapter.class.getName());

	private BootstrapContext bootstrapCtx;
	private JobExecutorActivation jobExecutorActivation;
	private Map<String, JobAcquisitionWork> jobAcquisitionMap;
	private JobAcquisitionWork defaultJobAcquistion;

	/**
	 * default constructor
	 */
	public JobExecutorResourceAdapter() {

	}

	@Override
	public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
		LOGGER.fine("Starting JobExecutorResourceAdapter with workmanager");
		this.bootstrapCtx = ctx;

		initJobAcquisitions();
	}

	@Override
	public void stop() {
		LOGGER.fine("Stopping JobExecutorResourceAdapter");
		this.bootstrapCtx = null;
	}

	@Override
	public void endpointActivation(MessageEndpointFactory mef, ActivationSpec activationSpec) throws ResourceException {
		if (!JobExecutorActivation.class.isAssignableFrom(activationSpec.getClass()))
			throw new ResourceException("Invalid activation spec type");

		LOGGER.fine("Activating endpoint JobExecutorActivation");

		jobExecutorActivation = (JobExecutorActivation) activationSpec;
		jobExecutorActivation.validate(); // jca contract
		jobExecutorActivation.setMessageEndpointFactory(mef);

	}

	@Override
	public void endpointDeactivation(MessageEndpointFactory mef, ActivationSpec activationSpec) {
		LOGGER.fine("Deactivating endpoint JobExecutorActivation");
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

	/* acqusition */

	private void initJobAcquisitions() {

		this.jobAcquisitionMap = new HashMap<String, JobAcquisitionWork>();

		for (JobConfigurationSectionImpl configuration : JobConfigurationAccessorImpl.getInstance().getSectionsMap()
				.values()) {
			String name = configuration.getName();
			JobAcquisitionWork jobAcquisition = new JobAcquisitionWork(name, configuration, this);
			jobAcquisitionMap.put(name, jobAcquisition);
			if (configuration.isDefault()) {
				defaultJobAcquistion = jobAcquisition;
			}
		}
	}

	public JobAcquisitionWork getDefaultJobAcquistion() {
		return defaultJobAcquistion;
	}

	public Map<String, JobAcquisitionWork> getJobAcquisitionMap() {
		return jobAcquisitionMap;
	}

}
