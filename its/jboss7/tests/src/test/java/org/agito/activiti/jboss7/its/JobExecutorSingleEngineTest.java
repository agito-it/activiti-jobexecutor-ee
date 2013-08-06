package org.agito.activiti.jboss7.its;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.JobEntityManager;
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
public class JobExecutorSingleEngineTest extends JobExecutorTestCase {

	private final static String PROCESS_ENGINE_1 = "ActivitiDS";

	@Override
	public String[] configureProcessEngineNames() {
		return new String[] { PROCESS_ENGINE_1 };
	}

	@Override
	public String configureJobAcquisitionForProcessEngine(String processEngine) {
		return null; // use default acquisition
	}

	@Deployment
	public static EnterpriseArchive createDeployment() {

		WebArchive jar = ShrinkWrap.create(WebArchive.class, "test.war")
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml").addPackages(true, "org/agito");
		EnterpriseArchive ear = ShrinkWrap
				.create(EnterpriseArchive.class, "activiti-jobexecutor-ee-jca-ear-" + getJobExecutorVersion() + ".ear")
				.addAsModule(AbstractContainerTest.getActivitiResourceAdapterArchive()).addAsModule(jar);

		return ear;
	}

	@Test
	public void testBasicJobExecutorOperation() throws Exception {
		try {
			setUp();

			CommandExecutor commandExecutor = processEngineConfigurations.get(PROCESS_ENGINE_1)
					.getCommandExecutorTxRequired();
			commandExecutor.execute(new Command<Void>() {
				public Void execute(CommandContext commandContext) {
					JobEntityManager jobManager = commandContext.getJobEntityManager();
					jobManager.send(createTweetMessage("message-one"));
					jobManager.send(createTweetMessage("message-two"));
					jobManager.send(createTweetMessage("message-three"));
					jobManager.send(createTweetMessage("message-four"));

					jobManager.schedule(createTweetTimer("timer-one", new Date()));
					jobManager.schedule(createTweetTimer("timer-one", new Date()));
					jobManager.schedule(createTweetTimer("timer-two", new Date()));
					return null;
				}
			});

			waitForJobExecutorToProcessAllJobs(30000L, 200L);

			Set<String> messages = new HashSet<String>(tweetHandler.getMessages());
			Set<String> expectedMessages = new HashSet<String>();
			expectedMessages.add("message-one");
			expectedMessages.add("message-two");
			expectedMessages.add("message-three");
			expectedMessages.add("message-four");
			expectedMessages.add("timer-one");
			expectedMessages.add("timer-two");

			Assert.assertEquals(new TreeSet<String>(expectedMessages), new TreeSet<String>(messages));

		} finally {
			tearDown();
		}

	}
}
