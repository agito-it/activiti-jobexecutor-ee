package org.agito.activiti.jobexecutor;

/**
 * Callback interface for notification of the registry, when new jobs are added in the process engine.
 * 
 * @author agito
 * 
 */
public interface JobWasAddedCallback {

	public void jobWasAdded();

}
