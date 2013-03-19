package org.agito.activiti.jobexecutor.ra.test;

import org.agito.activiti.jobexecutor.ra.impl.config.JobConfigurationAccessor;
import org.junit.Assert;
import org.junit.Test;

public class JobExecutorConfigurationTest {

	@Test
	public void testConfiguration() {
		JobConfigurationAccessor accessor = JobConfigurationAccessor.getInstance();
		Assert.assertTrue(accessor.getSectionsMap().size() > 0);
	}

}
