package org.agito.activiti.jboss7.its;

import javax.annotation.Resource;
import javax.naming.NamingException;

import org.activiti.engine.ProcessEngine;
import org.agito.activiti.jobexecutor.api.JobExecutorRegistryFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class SimpleTest extends AbstractContainerTest {

	@Deployment
	public static EnterpriseArchive createDeployment() {

		WebArchive jar = ShrinkWrap.create(WebArchive.class, "test.war")
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml").addPackages(true, "org/agito");

		EnterpriseArchive ear = ShrinkWrap
				.create(EnterpriseArchive.class, "activiti-jobexecutor-ee-jca-ear-" + getJobExecutorVersion() + ".ear")
				.addAsModule(getActivitiResourceAdapterArchive()).addAsModule(jar);

		return ear;
	}

	@Resource(mappedName = "env/ActivitiJobExecutor")
	private JobExecutorRegistryFactory jobExecutorRegistryFactory;

	@Test
	public void simple() throws NamingException {
		Assert.assertNotNull(jobExecutorRegistryFactory);

		ProcessEngine processEngine = getProcessEngineConfigurationImpl("ActivitiDS").buildProcessEngine();
		Assert.assertNotNull(processEngine);
	}

}
