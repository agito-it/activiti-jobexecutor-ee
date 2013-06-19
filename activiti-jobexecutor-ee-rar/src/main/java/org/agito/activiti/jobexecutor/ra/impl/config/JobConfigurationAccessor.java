package org.agito.activiti.jobexecutor.ra.impl.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.activiti.engine.ActivitiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobConfigurationAccessor {

	private final static Logger LOGGER = LoggerFactory.getLogger(JobConfigurationAccessor.class);

	private final static String JOB_CONFIGURATION_LOCATION = "jobexecutoree.properties";

	private static JobConfigurationAccessor INSTANCE;
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
	private final Map<String, JobConfigurationSection> sections;

	public static JobConfigurationAccessor getInstance() {
		if (!INITIALIZED) {
			initialize();
		}
		return INSTANCE;
	}

	private static synchronized void initialize() {
		if (!INITIALIZED) {
			INSTANCE = new JobConfigurationAccessor();
			INITIALIZED = true;
		}
	}

	private JobConfigurationAccessor() {

		sections = new HashMap<String, JobConfigurationSection>();

		if (!parseConfig(readFileAsString(JOB_CONFIGURATION_LOCATION))) {
			throw new ActivitiException("Job executor configuration has errors. Refer to earlier log entries.");
		}

	}

	public Map<String, JobConfigurationSection> getSectionsMap() {
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
				LOGGER.error(MessageFormat.format(
						"ERROR Job executor configuration > {0} > default defined more than once.", m.group(0)));
				break;
			}
			this.defaultName = m.group(1);
			LOGGER.debug("Job executor configuration > default=" + defaultName);
		}
		if (defaultName == null) {
			LOGGER.error("Job executor configuration misses default.");
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

			JobConfigurationSection section = getSection(name);

			if (JobConfigurationSection.FIELD_LOCK_TIME_IN_MILLIS.equals(property)) {
				isOk = isOk && validateJobConfigValue(name, JobConfigurationSection.FIELD_LOCK_TIME_IN_MILLIS, value);

				if (isOk)
					section.setLockTimeInMillis(Integer.valueOf(value));

				infoJobConfigValue(name, property, value);
			} else if (JobConfigurationSection.FIELD_WAIT_TIME_IN_MILLIS.equals(property)) {
				isOk = isOk && validateJobConfigValue(name, JobConfigurationSection.FIELD_WAIT_TIME_IN_MILLIS, value);

				if (isOk)
					section.setWaitTimeInMillis(Integer.valueOf(value));

				infoJobConfigValue(name, property, value);
			} else if (JobConfigurationSection.FIELD_MAX_JOBS_PER_ACQUISITION.equals(property)) {
				isOk = isOk
						&& validateJobConfigValue(name, JobConfigurationSection.FIELD_MAX_JOBS_PER_ACQUISITION, value);

				if (isOk)
					section.setMaxJobsPerAcquisition(Integer.valueOf(value));

				infoJobConfigValue(name, property, value);
			} else {
				LOGGER.error(MessageFormat.format(
						"Job executor configuration > name={0} > {1}={2} > unknown property ", name, property, value));
				isOk = false;
			}

		}

		return isOk;
	}

	private JobConfigurationSection getSection(String name) {
		JobConfigurationSection section = sections.get(name);
		if (section == null) {
			LOGGER.debug("Job executor configuration > name=" + name);
			section = new JobConfigurationSection();
			section.setName(name);
			section.setDefault(name.equals(defaultName));
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
		LOGGER.debug(MessageFormat.format("Job executor configuration > name={0} > {1}={2}", name, property, value));
	}

	private void errorInvalidJobConfigValue(String name, String field, String value) {
		LOGGER.error(MessageFormat.format(
				"Job executor configuration > name={0} > {1}={2} > value must be a valid positive integer.", name,
				field, value));
	}

	private boolean validateName(String name, Matcher m) {
		if (!name.matches(NAME_VALIDATION) && sections.get(name) == null) {
			LOGGER.error(MessageFormat.format(
					"ERROR Global configuration > invalid job configuration in '{0}'. Name does not match '{1}'",
					m.group(0), NAME_VALIDATION));
			return false;
		}
		return true;
	}

	private boolean validateGroupCount(Matcher m, int expectedGroupCount) {
		if (m.groupCount() != expectedGroupCount) {
			LOGGER.error("Invalid job configuration: " + m.group(0));
			return false;
		}
		return true;
	}

	private static String readFileAsString(String filePath) {

		StringBuffer sb = new StringBuffer();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(JobConfigurationAccessor.class.getClassLoader()
					.getResourceAsStream(filePath), "UTF-8"));
			for (int c = br.read(); c != -1; c = br.read())
				sb.append((char) c);
		} catch (Exception e) {
			throw new ActivitiException("Couldn't read file " + filePath + ": " + e.getMessage());
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				// ignore
			}
		}
		return sb.toString();
	}

}
