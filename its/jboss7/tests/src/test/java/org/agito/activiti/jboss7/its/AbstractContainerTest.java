package org.agito.activiti.jboss7.its;

import java.util.Properties;

import org.agito.activiti.jboss7.engine.impl.JobExecutorProcessEngineConfiguration;
import org.agito.activiti.jboss7.engine.impl.JtaProcessEngineConfiguration;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

public class AbstractContainerTest {

	private static String JOBEXECUTOR_EE_VERSION;

	private static ResourceAdapterArchive CACHED_JCA_ASSET;

	public JobExecutorProcessEngineConfiguration getProcessEngineConfigurationImpl(String processEngineName) {
		JtaProcessEngineConfiguration ret = new JtaProcessEngineConfiguration();
		ret.setProcessEngineName(processEngineName);
		ret.setDataSourceJndiName("java:jboss/datasources/" + processEngineName);
		ret.setJobExecutorActivate(true);
		ret.setDatabaseSchemaUpdate("true");
		return ret;
	}

	public static ResourceAdapterArchive getActivitiResourceAdapterArchive() {

		if (CACHED_JCA_ASSET != null) {
			return CACHED_JCA_ASSET;
		} else {
			ResourceAdapterArchive archive = Maven.resolver().loadPomFromFile("pom.xml")
					.resolve("org.agito:activiti-jobexecutor-ee-jca-rar:rar:" + getJobExecutorVersion())
					.withoutTransitivity().asSingle(ResourceAdapterArchive.class);

			if (archive == null) {
				throw new RuntimeException("could not resolve org.agito:activiti-jobexecutor-ee-jca-rar");
			} else {
				CACHED_JCA_ASSET = archive;
				return CACHED_JCA_ASSET;
			}
		}

	}

	protected static String getJobExecutorVersion() {
		if (JOBEXECUTOR_EE_VERSION == null)
			JOBEXECUTOR_EE_VERSION = readCurrentVersion();
		return JOBEXECUTOR_EE_VERSION;
	}

	private static String readCurrentVersion() {
		Properties props = new Properties();
		try {
			props.load(AbstractContainerTest.class.getResourceAsStream("/build.properties"));
		} catch (Exception e) {
			throw new RuntimeException("Error reading build.properties. Run maven build first.", e);
		}
		return props.getProperty("current.version");
	}
}
