package org.agito.activiti.jobexecutor.impl.config;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.activiti.engine.impl.util.IoUtil;

public class JobConfigurationAccessorImpl {

	private final static Logger LOGGER = Logger.getLogger(JobConfigurationAccessorImpl.class.getName());

	private final static String JOB_CONFIGURATION_LOCATION = "jobexecutoree.properties";

	private static JobConfigurationAccessorImpl INSTANCE;
	private static boolean INITIALIZED;

	/**
	 * Default configuration
	 * <p/>
	 * regexp: jobexecutoree\.default=(.*)
	 * <p/>
	 * group1 = value
	 */
	private final static String DEFAULT_CONFIGURATION = "^jobexecutoree\\.default=(.*)$";

	/**
	 * Job executor configuration properties
	 * <p/>
	 * jobexecutoree\.cfg\.([^\.]*)\.([^=]*)=(.*)
	 * <p/>
	 * g1=section g2=property g3=value
	 */
	private final static String CONFIGURATION_PROPS = "^jobexecutoree\\.cfg\\.([^\\.]*)\\.([^=]*)=(.*)$";

	private final static String NAME_VALIDATION = "[A-Za-z0-9_]{3,64}";

	private String defaultName;
	private final Map<String, JobConfigurationSectionImpl> sections;

	public static JobConfigurationAccessorImpl getInstance() {
		if (!INITIALIZED) {
			initialize();
		}
		return INSTANCE;
	}

	private static synchronized void initialize() {
		if (!INITIALIZED) {
			INSTANCE = new JobConfigurationAccessorImpl();
			INITIALIZED = true;
		}
	}

	private JobConfigurationAccessorImpl() {

		sections = new HashMap<String, JobConfigurationSectionImpl>();

		parseConfig(IoUtil.readFileAsString(JOB_CONFIGURATION_LOCATION));

	}

	public Map<String, JobConfigurationSectionImpl> getSectionsMap() {
		return this.sections;
	}

	private boolean parseConfig(String config) {

		boolean isOk = true;

		// default
		Pattern p = Pattern.compile(DEFAULT_CONFIGURATION, Pattern.MULTILINE);
		Matcher m = p.matcher(config);

		while (m.find()) {
			isOk = isOk && validateGroupCount(m, 1);
			if (this.defaultName != null) {
				isOk = false;
				LOGGER.severe(MessageFormat.format(
						"ERROR Job executor configuration > {0} > default defined more than once.", m.group(0)));
				break;
			}
			this.defaultName = m.group(1);
			LOGGER.info("Job executor configuration > default=" + defaultName);
		}
		if (defaultName == null) {
			LOGGER.severe("Job executor configuration misses default.");
			isOk = false;
		}

		// properties
		p = Pattern.compile(CONFIGURATION_PROPS, Pattern.MULTILINE);
		m = p.matcher(config);
		while (m.find()) {
			isOk = isOk && validateGroupCount(m, 3);
			String name = m.group(1);
			String property = m.group(2);
			String value = m.group(3);

			isOk = isOk && validateName(name, m);

			JobConfigurationSectionImpl section = getSection(name);

			if (JobConfigurationSectionImpl.FIELD_LOCK_TIME_IN_MILLIS.equals(property)) {
				isOk = isOk
						&& validateJobConfigValue(name, JobConfigurationSectionImpl.FIELD_LOCK_TIME_IN_MILLIS, value);

				if (isOk)
					section.setLockTimeInMillis(Integer.valueOf(value));

				infoJobConfigValue(name, property, value);
			} else if (JobConfigurationSectionImpl.FIELD_WAIT_TIME_IN_MILLIS.equals(property)) {
				isOk = isOk
						&& validateJobConfigValue(name, JobConfigurationSectionImpl.FIELD_WAIT_TIME_IN_MILLIS, value);

				if (isOk)
					section.setWaitTimeInMillis(Integer.valueOf(value));

				infoJobConfigValue(name, property, value);
			} else if (JobConfigurationSectionImpl.FIELD_MAX_JOBS_PER_ACQUISITION.equals(property)) {
				isOk = isOk
						&& validateJobConfigValue(name, JobConfigurationSectionImpl.FIELD_MAX_JOBS_PER_ACQUISITION,
								value);

				if (isOk)
					section.setMaxJobsPerAcquisition(Integer.valueOf(value));

				infoJobConfigValue(name, property, value);
			} else {
				LOGGER.severe(MessageFormat.format(
						"Job executor configuration > name={0} > {1}={2} > unknown property ", name, property, value));
				isOk = false;
			}

		}

		return isOk;
	}

	private JobConfigurationSectionImpl getSection(String name) {
		JobConfigurationSectionImpl section = sections.get(name);
		if (section == null) {
			LOGGER.info("Job executor configuration > name=" + name);
			section = new JobConfigurationSectionImpl();
			section.setName(name);
			sections.put(name, section);
		}
		return section;
	}

	private boolean validateJobConfigValue(String name, String field, String value) {

		if (value == null || value.trim().length() == 0) {
			errorInvalidJobConfigValue(name, field, value);
			return false;
		}

		int intValue = -1;
		try {
			intValue = Integer.valueOf(value);
		} catch (NumberFormatException e) {
			errorInvalidJobConfigValue(name, field, value);
			return false;
		}

		if (intValue < 0) {
			errorInvalidJobConfigValue(name, field, value);
			return false;
		}

		return true;

	}

	private void infoJobConfigValue(String name, String property, String value) {
		LOGGER.info(MessageFormat.format("Job executor configuration > name={0} > {1}={2}", name, property, value));
	}

	private void errorInvalidJobConfigValue(String name, String field, String value) {
		LOGGER.severe(MessageFormat.format(
				"Job executor configuration > name={0} > {1}={2} > value must be a valid positive integer.", name,
				field, value));
	}

	private boolean validateName(String name, Matcher m) {
		if (!name.matches(NAME_VALIDATION) && sections.get(name) == null) {
			LOGGER.severe(MessageFormat.format(
					"ERROR Global configuration > invalid job configuration in '{0}'. Name does not match '{1}'",
					m.group(0), NAME_VALIDATION));
			return false;
		}
		return true;
	}

	private boolean validateGroupCount(Matcher m, int expectedGroupCount) {
		if (m.groupCount() != expectedGroupCount) {
			LOGGER.severe("Invalid job configuration: " + m.group(0));
			return false;
		}
		return true;
	}

}
