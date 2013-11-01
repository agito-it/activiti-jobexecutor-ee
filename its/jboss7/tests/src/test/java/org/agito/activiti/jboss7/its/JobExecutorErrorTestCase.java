package org.agito.activiti.jboss7.its;

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.jobexecutor.JobHandler;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.impl.persistence.entity.JobEntityManager;
import org.activiti.engine.impl.persistence.entity.MessageEntity;
import org.agito.activiti.jboss7.its.JobExecutorErrorTestCase.ErrorJobHandler;
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
public class JobExecutorErrorTestCase extends AbstractJobExecutorTestCase<ErrorJobHandler> {

	private final static String PROCESS_ENGINE_1 = "ActivitiDS";

	@Override
	public String[] configureProcessEngineNames() {
		return new String[] { PROCESS_ENGINE_1 };
	}

	@Override
	public String configureJobAcquisitionForProcessEngine(String processEngine) {
		return null; // use default acquisition
	}

	@Override
	protected ErrorJobHandler initJobHandler() {
		return new ErrorJobHandler();
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
	public void testErrorMessageOperation() throws Exception {
		try {
			setUp();

			CommandExecutor commandExecutor = processEngineConfigurations.get(PROCESS_ENGINE_1)
					.getCommandExecutor();

			// enable errors
			jobHandler.errorMode = 1;

			final String jobId = commandExecutor.execute(new Command<String>() {
				public String execute(CommandContext commandContext) {
					JobEntityManager jobManager = commandContext.getJobEntityManager();

					MessageEntity message = new MessageEntity();
					message.setJobHandlerType("error");
					jobManager.send(message);

					return message.getId();
				}
			});

			waitForJobExecutorToProcessAllJobs(30000L, 200L);

			// disable errors
			jobHandler.errorMode = 0;

			commandExecutor.execute(new Command<Void>() {
				public Void execute(CommandContext commandContext) {

					MessageEntity job = (MessageEntity) commandContext.getDbSqlSession().createJobQuery().jobId(jobId)
							.singleResult();
					Assert.assertNotNull(job);
					Assert.assertEquals(0, job.getRetries());

					job.setRetries(1);

					return null;
				}
			});

			processEngineConfigurations.get(PROCESS_ENGINE_1).getJobExecutor().jobWasAdded();

			waitForJobExecutorToProcessAllJobs(30000L, 200L);

			commandExecutor.execute(new Command<Void>() {
				public Void execute(CommandContext commandContext) {

					JobEntity job = (JobEntity) commandContext.getDbSqlSession().createJobQuery().jobId(jobId)
							.singleResult();
					Assert.assertNull(job);

					return null;
				}
			});

		} finally {
			tearDown();
		}

	}

	protected static class ErrorJobHandler implements JobHandler {

		protected int errorMode;

		@Override
		public String getType() {
			return "error";
		}

		@Override
		public void execute(JobEntity job, String configuration, ExecutionEntity execution,
				CommandContext commandContext) {
			switch (errorMode) {
			case 1:
				throw new RuntimeException("Testing an error");
			default:
				break;
			}
		}
	}
}
