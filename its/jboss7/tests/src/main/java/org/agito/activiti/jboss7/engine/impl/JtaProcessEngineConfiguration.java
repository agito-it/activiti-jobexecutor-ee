package org.agito.activiti.jboss7.engine.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.naming.InitialContext;
import javax.transaction.TransactionManager;

import org.activiti.engine.impl.cfg.jta.JtaTransactionContextFactory;
import org.activiti.engine.impl.interceptor.CommandContextInterceptor;
import org.activiti.engine.impl.interceptor.CommandInterceptor;
import org.activiti.engine.impl.interceptor.JtaTransactionInterceptor;
import org.activiti.engine.impl.interceptor.LogInterceptor;

public class JtaProcessEngineConfiguration extends JobExecutorProcessEngineConfiguration {

	public static String[] TRANSACTION_MANAGER_LOCATIONS = { "java:appserver/TransactionManager", // glassfish 3.1
			"java:/TransactionManager", // jboss 6/7
			"java:jboss/TransactionManager", // jboss 7
	};

	@Override
	protected void init() {
		initTransactionManager();
		super.init();
	}

	protected void initTransactionManager() {
		if (transactionManager == null) {
			if (transactionManagerLookup == null) {
				for (String transactionManagerLocation : TRANSACTION_MANAGER_LOCATIONS) {
					try {
						transactionManager = InitialContext.doLookup(transactionManagerLocation);
						if (transactionManager != null) {
							break;
						}
					} catch (Exception e) {
						// ignore
					}
				}
			} else {
				try {
					transactionManager = InitialContext.doLookup(transactionManagerLookup);
				} catch (Exception e) {
					throw new IllegalStateException("Could not lookup a transaction manager using '"
							+ transactionManagerLookup + "'", e);
				}
			}
			if (transactionManager == null) {
				throw new IllegalStateException(
						"Could not lookup a transaction manager using the following known locations: "
								+ Arrays.toString(TRANSACTION_MANAGER_LOCATIONS));
			}
		}
	}

	protected void initTransactionContextFactory() {
		if (transactionContextFactory == null) {
			transactionContextFactory = new JtaTransactionContextFactory(transactionManager);
		}
	}

	/** the string used to lookup the transaction manager */
	protected String transactionManagerLookup;

	/** the jta transaction manager */
	protected TransactionManager transactionManager;

	@Override
	protected Collection<? extends CommandInterceptor> getDefaultCommandInterceptorsTxRequired() {
		List<CommandInterceptor> defaultCommandInterceptorsTxRequired = new ArrayList<CommandInterceptor>();
		defaultCommandInterceptorsTxRequired.add(new LogInterceptor());
		defaultCommandInterceptorsTxRequired.add(new JtaTransactionInterceptor(transactionManager, false));
		defaultCommandInterceptorsTxRequired.add(new CommandContextInterceptor(commandContextFactory, this));
		return defaultCommandInterceptorsTxRequired;
	}

	@Override
	protected Collection<? extends CommandInterceptor> getDefaultCommandInterceptorsTxRequiresNew() {
		List<CommandInterceptor> defaultCommandInterceptorsTxRequiresNew = new ArrayList<CommandInterceptor>();
		defaultCommandInterceptorsTxRequiresNew.add(new LogInterceptor());
		defaultCommandInterceptorsTxRequiresNew.add(new JtaTransactionInterceptor(transactionManager, true));
		defaultCommandInterceptorsTxRequiresNew.add(new CommandContextInterceptor(commandContextFactory, this));
		return defaultCommandInterceptorsTxRequiresNew;
	}

	public TransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setTransactionManagerLookup(String transactionManagerLookup) {
		this.transactionManagerLookup = transactionManagerLookup;
	}

	public String getTransactionManagerLookup() {
		return transactionManagerLookup;
	}

}
