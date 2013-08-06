package org.agito.activiti.jboss7.engine.impl;

import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.agito.activiti.jobexecutor.JobExecutorEE;

public abstract class JobExecutorProcessEngineConfiguration extends ProcessEngineConfigurationImpl {

	protected String jobAcquisitionName;

	@Override
	public void initJobExecutor() {
		if (jobExecutor == null) {
			
			JobExecutorEE jobExecutorEE = new JobExecutorEE();
			jobExecutorEE.setAcquisitionName(jobAcquisitionName);
			
			jobExecutor = jobExecutorEE;
		}
		super.initJobExecutor();
	}

	public String getJobAcquisitionName() {
		return jobAcquisitionName;
	}

	public JobExecutorProcessEngineConfiguration setJobAcquisitionName(String jobAcquisitionName) {
		this.jobAcquisitionName = jobAcquisitionName;
		return this;
	}

}
