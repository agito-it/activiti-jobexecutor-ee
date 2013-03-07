package org.agito.activiti.jobexecutor;

import java.io.PrintWriter;
import java.util.Set;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;

import org.agito.activiti.jobexecutor.impl.DefaultConnectionManager;
import org.agito.activiti.jobexecutor.impl.JobExecutorManagedConnection;
import org.agito.activiti.jobexecutor.impl.JobExecutorRegistryFactoryImpl;

public class JobExecutorManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {

	private static final long serialVersionUID = -1314691599853676047L;

	/* Managed Connection Factory */

	@Override
	public Object createConnectionFactory() throws ResourceException {
		return createConnectionFactory(new DefaultConnectionManager());
	}

	@Override
	public Object createConnectionFactory(ConnectionManager cm) throws ResourceException {
		return new JobExecutorRegistryFactoryImpl(cm, this);
	}

	@Override
	public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo info)
			throws ResourceException {
		return new JobExecutorManagedConnection(this, subject, info, getLogWriter());
	}

	@Override
	public ManagedConnection matchManagedConnections(@SuppressWarnings("rawtypes") Set connectionSet, Subject subject,
			ConnectionRequestInfo info) throws ResourceException {
		// connection pooling not supported
		throw new NotSupportedException();
	}

	/* log writer */

	private PrintWriter logWriter;

	@Override
	public PrintWriter getLogWriter() throws ResourceException {
		return logWriter;
	}

	@Override
	public void setLogWriter(PrintWriter logWriter) throws ResourceException {
		this.logWriter = logWriter;
	}

	/* resource adapter association */

	private JobExecutorResourceAdapter resourceAdapter;

	@Override
	public JobExecutorResourceAdapter getResourceAdapter() {
		return resourceAdapter;
	}

	@Override
	public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
		if (!JobExecutorResourceAdapter.class.isAssignableFrom(ra.getClass()))
			throw new ResourceException("ResourceAdapter is not of type " + JobExecutorResourceAdapter.class.getName());
		this.resourceAdapter = (JobExecutorResourceAdapter) ra;
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
