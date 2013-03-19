package org.agito.activiti.jobexecutor.ra.impl.config;

public class JobConfigurationSection {

	public final static int DEFAULT_MAX_JOBS_PER_ACQUISITION = 3;
	public final static int DEFAULT_LOCK_TIME_IN_MILLIS = 30000;
	public final static int DEFAULT_WAIT_TIME_IN_MILLIS = 5000;

	public final static String FIELD_MAX_JOBS_PER_ACQUISITION = "maxJobsPerAcquisition";
	public final static String FIELD_LOCK_TIME_IN_MILLIS = "lockTimeInMillis";
	public final static String FIELD_WAIT_TIME_IN_MILLIS = "waitTimeInMillis";

	private String name;
	private boolean isDefault;

	private int maxJobsPerAcquisition = DEFAULT_MAX_JOBS_PER_ACQUISITION;
	private int lockTimeInMillis = DEFAULT_LOCK_TIME_IN_MILLIS;
	private int waitTimeInMillis = DEFAULT_WAIT_TIME_IN_MILLIS;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	public boolean isDefault() {
		return isDefault;
	}

	public int getMaxJobsPerAcquisition() {
		return maxJobsPerAcquisition;
	}

	public void setMaxJobsPerAcquisition(int maxJobsPerAcquisition) {
		this.maxJobsPerAcquisition = maxJobsPerAcquisition;
	}

	public int getLockTimeInMillis() {
		return lockTimeInMillis;
	}

	public void setLockTimeInMillis(int lockTimeInMillis) {
		this.lockTimeInMillis = lockTimeInMillis;
	}

	public int getWaitTimeInMillis() {
		return waitTimeInMillis;
	}

	public void setWaitTimeInMillis(int waitTimeInMillis) {
		this.waitTimeInMillis = waitTimeInMillis;
	}

}
