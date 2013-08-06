package org.agito.activiti.jobexecutor.config.test;

import org.agito.activiti.jobexecutor.config.JobConfigurationAccessor;
import org.junit.Assert;
import org.junit.Test;

public class JobExecutorConfigurationTest {

	@Test
	public void testConfiguration() {
		JobConfigurationAccessor accessor = new JobConfigurationAccessor(JobExecutorConfigurationTest.class
				.getClassLoader().getResourceAsStream(JobConfigurationAccessor.JOB_CONFIGURATION_LOCATION));
		Assert.assertEquals(accessor.getSectionsMap().size(), 2);
	}

}
