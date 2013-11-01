package org.agito.activiti.jboss7.engine.impl;

import org.activiti.engine.impl.interceptor.CommandInterceptor;

public class StandaloneExecutorProcessEngineConfiguration extends JobExecutorProcessEngineConfiguration {

	@Override
	protected CommandInterceptor createTransactionInterceptor() {
		return null;
	}

}
