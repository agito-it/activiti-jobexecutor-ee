package org.agito.activiti.jobexecutor.ra.impl;

import java.io.Serializable;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

import org.agito.activiti.jobexecutor.api.JobExecutorRegistry;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistryFactory;
import org.agito.activiti.jobexecutor.ra.JobExecutorManagedConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobExecutorRegistryFactoryImpl implements JobExecutorRegistryFactory, Serializable, Referenceable {

	private final static Logger LOGGER = LoggerFactory.getLogger(JobExecutorRegistryFactoryImpl.class);

	private static final long serialVersionUID = -2526727522162902608L;

	private final ConnectionManager cm;
	private final JobExecutorManagedConnectionFactory mcf;

	public JobExecutorRegistryFactoryImpl(final ConnectionManager cm, final JobExecutorManagedConnectionFactory mcf) {
		LOGGER.trace("JobExecutorRegistryFactoryImpl(ConnectionManager, JobExecutorManagedConnectionFactory)");
		this.cm = cm;
		this.mcf = mcf;
	}

	/* connection */

	@Override
	public JobExecutorRegistry getRegistry() throws ResourceException {
		LOGGER.trace("getRegistry(JobExecutorInfo)");
		return (JobExecutorRegistry) this.cm.allocateConnection(mcf, null);
	}

	/* reference */

	private Reference reference;

	public void setReference(Reference ref) { // setReference is called by the deployment code
		LOGGER.trace("setReference(Reference)");
		reference = ref;
	}

	public Reference getReference() throws NamingException { // getReference is called by the JNDI provider during Context.bind
		LOGGER.trace("getReference()");
		return reference;
	}

}
