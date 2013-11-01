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
public class JobExecutorMultiEnginesTest extends AbstractTweetTest {

	private final static String PROCESS_ENGINE_1 = "ActivitiDS";
	private final static String PROCESS_ENGINE_2 = "ActivitiDS_2";
	private final static String PROCESS_ENGINE_3 = "ActivitiDS_3";

	@Override
	public String[] configureProcessEngineNames() {
		return new String[] { PROCESS_ENGINE_1, PROCESS_ENGINE_2, PROCESS_ENGINE_3 };
	}

	@Override
	public String configureJobAcquisitionForProcessEngine(String processEngine) {

		if (PROCESS_ENGINE_3.equals(processEngine)) {
			return JOB_ACQUISITION_2;
		} else {
			return JOB_ACQUISITION_1;
		}

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

			CommandExecutor commandExecutor1 = processEngineConfigurations.get(PROCESS_ENGINE_1).getCommandExecutor();
			commandExecutor1.execute(new Command<Void>() {
				public Void execute(CommandContext commandContext) {
					JobEntityManager jobManager = commandContext.getJobEntityManager();
					jobManager.send(createTweetMessage("message-1-one"));
					jobManager.send(createTweetMessage("message-1-two"));
					jobManager.send(createTweetMessage("message-1-three"));
					jobManager.send(createTweetMessage("message-1-four"));

					jobManager.schedule(createTweetTimer("timer-1-one", new Date()));
					jobManager.schedule(createTweetTimer("timer-1-one", new Date()));
					jobManager.schedule(createTweetTimer("timer-1-two", new Date()));
					return null;
				}
			});

			CommandExecutor commandExecutor2 = processEngineConfigurations.get(PROCESS_ENGINE_2).getCommandExecutor();
			commandExecutor2.execute(new Command<Void>() {
				public Void execute(CommandContext commandContext) {
					JobEntityManager jobManager = commandContext.getJobEntityManager();
					jobManager.send(createTweetMessage("message-2-one"));
					jobManager.send(createTweetMessage("message-2-two"));
					jobManager.send(createTweetMessage("message-2-three"));
					jobManager.send(createTweetMessage("message-2-four"));

					jobManager.schedule(createTweetTimer("timer-2-one", new Date()));
					jobManager.schedule(createTweetTimer("timer-2-one", new Date()));
					jobManager.schedule(createTweetTimer("timer-2-two", new Date()));
					return null;
				}
			});

			CommandExecutor commandExecutor3 = processEngineConfigurations.get(PROCESS_ENGINE_3).getCommandExecutor();
			commandExecutor3.execute(new Command<Void>() {
				public Void execute(CommandContext commandContext) {
					JobEntityManager jobManager = commandContext.getJobEntityManager();
					jobManager.send(createTweetMessage("message-3-one"));
					jobManager.send(createTweetMessage("message-3-two"));
					jobManager.send(createTweetMessage("message-3-three"));
					jobManager.send(createTweetMessage("message-3-four"));

					jobManager.schedule(createTweetTimer("timer-3-one", new Date()));
					jobManager.schedule(createTweetTimer("timer-3-one", new Date()));
					jobManager.schedule(createTweetTimer("timer-3-two", new Date()));
					return null;
				}
			});

			waitForJobExecutorToProcessAllJobs(30000L, 200L);

			Set<String> messages = new HashSet<String>(jobHandler.getMessages());
			Set<String> expectedMessages = new HashSet<String>();
			expectedMessages.add("message-1-one");
			expectedMessages.add("message-1-two");
			expectedMessages.add("message-1-three");
			expectedMessages.add("message-1-four");
			expectedMessages.add("timer-1-one");
			expectedMessages.add("timer-1-two");

			expectedMessages.add("message-2-one");
			expectedMessages.add("message-2-two");
			expectedMessages.add("message-2-three");
			expectedMessages.add("message-2-four");
			expectedMessages.add("timer-2-one");
			expectedMessages.add("timer-2-two");

			expectedMessages.add("message-3-one");
			expectedMessages.add("message-3-two");
			expectedMessages.add("message-3-three");
			expectedMessages.add("message-3-four");
			expectedMessages.add("timer-3-one");
			expectedMessages.add("timer-3-two");

			Assert.assertEquals(new TreeSet<String>(expectedMessages), new TreeSet<String>(messages));

		} finally {
			tearDown();
		}

	}
}
