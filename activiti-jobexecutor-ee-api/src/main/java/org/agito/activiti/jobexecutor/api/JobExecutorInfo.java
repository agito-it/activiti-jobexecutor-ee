package org.agito.activiti.jobexecutor.api;

import javax.resource.spi.ConnectionRequestInfo;

public class JobExecutorInfo implements ConnectionRequestInfo {

	protected String jobExecutorId;

	public JobExecutorInfo() {
	}

	public JobExecutorInfo(String jobExecutorId) {
		this.jobExecutorId = jobExecutorId;
	}

	public String getJobExecutorId() {
		return jobExecutorId;
	}

	public void setJobExecutorId(String jobExecutorId) {
		this.jobExecutorId = jobExecutorId;
	}

	/* equals / hashCode */

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jobExecutorId == null) ? 0 : jobExecutorId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JobExecutorInfo other = (JobExecutorInfo) obj;
		if (jobExecutorId == null) {
			if (other.jobExecutorId != null)
				return false;
		} else if (!jobExecutorId.equals(other.jobExecutorId))
			return false;
		return true;
	}

}
