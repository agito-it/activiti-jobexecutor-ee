package org.agito.activiti.jobexecutor.impl;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;

/**
 * Default Connection Manager Implementation.
 * 
 * @author agito
 * 
 */
public class DefaultConnectionManager implements ConnectionManager {

	private static final long serialVersionUID = -3102308224606632055L;

	@Override
	public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo cri) throws ResourceException {
		ManagedConnection mc = mcf.createManagedConnection(null, cri);
		return mc.getConnection(null, cri);
	}

}
