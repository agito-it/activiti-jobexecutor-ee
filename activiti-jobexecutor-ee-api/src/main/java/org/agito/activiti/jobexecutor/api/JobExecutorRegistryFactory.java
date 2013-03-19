package org.agito.activiti.jobexecutor.api;

import javax.resource.ResourceException;

/**
 * Interface for the registry factory, that can be looked up from JNDI.
 * 
 * @author agito
 * 
 */
public interface JobExecutorRegistryFactory {

	public final static String JNDI = "env/ActivitiJobExecutor";

	public JobExecutorRegistry getRegistry() throws ResourceException;

}
