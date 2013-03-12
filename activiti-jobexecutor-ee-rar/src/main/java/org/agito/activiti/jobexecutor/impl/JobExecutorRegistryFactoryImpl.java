package org.agito.activiti.jobexecutor.impl;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

import org.agito.activiti.jobexecutor.JobExecutorManagedConnectionFactory;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistry;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistryFactory;
import org.agito.activiti.jobexecutor.api.JobExecutorInfo;

public class JobExecutorRegistryFactoryImpl implements JobExecutorRegistryFactory, Serializable, Referenceable {
	
	private final static Logger LOGGER = Logger.getLogger(JobExecutorRegistryFactoryImpl.class.getName());

	private static final long serialVersionUID = -2526727522162902608L;

	private final ConnectionManager cm;
	private final JobExecutorManagedConnectionFactory mcf;

	public JobExecutorRegistryFactoryImpl(final ConnectionManager cm, final JobExecutorManagedConnectionFactory mcf) {
		LOGGER.finer("JobExecutorRegistryFactoryImpl(ConnectionManager, JobExecutorManagedConnectionFactory)");
		this.cm = cm;
		this.mcf = mcf;
	}

	/* connection */

	@Override
	public JobExecutorRegistry getRegistry(JobExecutorInfo properties) throws ResourceException {
		LOGGER.finer("getRegistry(JobExecutorInfo)");
		return (JobExecutorRegistry) this.cm.allocateConnection(mcf, properties);
	}

	/* reference */

	private Reference reference;

	public void setReference(Reference ref) { // setReference is called by the deployment code
		LOGGER.finer("setReference(Reference)");
		reference = ref;
	}

	public Reference getReference() throws NamingException { // getReference is called by the JNDI provider during Context.bind
		LOGGER.finer("getReference()");
		return reference;
	}

}
