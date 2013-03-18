package org.agito.activiti.jobexecutor;

import org.agito.activiti.jobexecutor.impl.config.JobConfigurationAccessorImpl;
import org.junit.Assert;
import org.junit.Test;

public class JobExecutorConfigurationTest {

	@Test
	public void test() {
		JobConfigurationAccessorImpl accessor = JobConfigurationAccessorImpl.getInstance();
		Assert.assertTrue(accessor.getSectionsMap().size() > 0);
	}

}
