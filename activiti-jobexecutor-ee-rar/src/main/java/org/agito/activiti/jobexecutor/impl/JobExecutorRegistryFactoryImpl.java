package org.agito.activiti.jobexecutor.impl;

import java.io.Serializable;

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

	private static final long serialVersionUID = -2526727522162902608L;

	private final ConnectionManager cm;
	private final JobExecutorManagedConnectionFactory mcf;

	public JobExecutorRegistryFactoryImpl(final ConnectionManager cm, final JobExecutorManagedConnectionFactory mcf) {
		this.cm = cm;
		this.mcf = mcf;
	}

	/* connection */

	@Override
	public JobExecutorRegistry getRegistry(JobExecutorInfo properties) throws ResourceException {
		return (JobExecutorRegistry) this.cm.allocateConnection(mcf, properties);
	}

	/* reference */

	private Reference reference;

	public void setReference(Reference ref) { // setReference is called by the deployment code
		reference = ref;
	}

	public Reference getReference() throws NamingException { // getReference is called by the JNDI provider during Context.bind
		return reference;
	}

}
