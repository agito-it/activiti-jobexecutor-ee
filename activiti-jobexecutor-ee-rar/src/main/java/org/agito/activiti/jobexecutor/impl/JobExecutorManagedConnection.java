package org.agito.activiti.jobexecutor.impl;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.agito.activiti.jobexecutor.JobExecutorManagedConnectionFactory;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistry;
import org.agito.activiti.jobexecutor.api.JobExecutorInfo;

public class JobExecutorManagedConnection implements ManagedConnection {

	public JobExecutorManagedConnection(final JobExecutorManagedConnectionFactory mcf, final Subject subject,
			final ConnectionRequestInfo info, final PrintWriter defaultLogWriter) {
		this.logWriter = defaultLogWriter;
	}

	/* actual connection handle */

	@Override
	public void associateConnection(Object connection) throws ResourceException {
		throw new NotSupportedException("Connection Association not supported.");
	}

	/**
	 * Container removes managedConnection from pool and invokes this method to release all physical resources. Normally invoked when a certain max idle time is execeeded.
	 */
	@Override
	public void destroy() throws ResourceException {
		this.logWriter.print(this + " destroy");
		cleanup();
	}

	/**
	 * Right before a connection is put back to pool. Invoked when a conenction handle is closed propertly. Physical resources can remain open.
	 */
	@Override
	public void cleanup() throws ResourceException {
		this.logWriter.print(this + " cleanup");
	}

	@Override
	public Object getConnection(Subject subject, ConnectionRequestInfo info) throws ResourceException {
		this.logWriter.print(this + " getConnection");

		if (info == null || info instanceof JobExecutorInfo)
			return new JobExecutorRegistryImpl(this, (JobExecutorInfo) info);

		throw new ResourceException("Unknown ConnectionRequestInfo type: " + info.getClass().getName());

	}

	/**
	 * LogWriter might be set by ManagedConnectionFactory but also from the server itself.
	 */
	private PrintWriter logWriter;

	public PrintWriter getLogWriter() throws ResourceException {
		return this.logWriter;
	}

	public void setLogWriter(PrintWriter logWriter) throws ResourceException {
		this.logWriter = logWriter;
	}

	/**
	 * A connection handle has triggered close event.
	 * 
	 * Container is registered as listener and needs to be informed to clean pools.
	 */
	public void closeHandle(JobExecutorRegistry connection) {
		this.logWriter.print(this + " closeHandle: ");
		for (ConnectionEventListener listener : this.listenerList) {
			ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
			event.setConnectionHandle(connection);
			listener.connectionClosed(event);
		}
	}

	/**
	 * All listeners of the container need to be triggered when an error occurs.
	 */
	public void errorOccured() {
		this.logWriter.print(this + " error: ");
		for (ConnectionEventListener listener : this.listenerList) {
			ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_ERROR_OCCURRED);
			listener.connectionClosed(event);
		}
	}

	private final List<ConnectionEventListener> listenerList = new LinkedList<ConnectionEventListener>();

	public void addConnectionEventListener(ConnectionEventListener listener) {
		this.listenerList.add(listener);
	}

	public void removeConnectionEventListener(ConnectionEventListener listener) {
		this.listenerList.remove(listener);
	}

	/* unsupported operations */

	@Override
	public ManagedConnectionMetaData getMetaData() throws ResourceException {
		throw new NotSupportedException("MetaData not supported");
	}

	@Override
	public LocalTransaction getLocalTransaction() throws ResourceException {
		throw new NotSupportedException("LocalTransaction not supported");
	}

	@Override
	public XAResource getXAResource() throws ResourceException {
		throw new NotSupportedException("XATResource not supported");
	}

}
