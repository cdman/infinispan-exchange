package com.blogspot.hypefree.infinispantest;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.distribution.group.Grouper;
import org.infinispan.executors.ExecutorFactory;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

public final class Constants {
	// don't use strings directly to avoid inlining
	private final static String ORDERS = "orders";
	private final static String TRANSACTIONS = "transactions";

	public static String getOrdersCacheName() {
		return ORDERS;
	}

	public static String getTransactions() {
		return TRANSACTIONS;
	}

	public static EmbeddedCacheManager createCacheManager() {
		// https://docs.jboss.org/author/display/ISPN/Getting+Started+Guide+-+Clustered+Cache+in+Java+SE
		System.setProperty("jgroups.bind_addr", "127.0.0.1");
		System.setProperty("java.net.preferIPv4Stack", "true");

		return new DefaultCacheManager(GlobalConfigurationBuilder
				.defaultClusteredBuilder().asyncListenerExecutor()
				.factory(new SingleThreadedExecutorFactory()).transport()
				.build(), new ConfigurationBuilder().invocationBatching()
				.enable().clustering().cacheMode(CacheMode.DIST_SYNC).hash()
				.numOwners(2).groups().addGrouper(new ConstantGrouper())
				.build());
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Cache<K, V> getCache(
			EmbeddedCacheManager cacheManager, String name) {
		return (Cache<K, V>) cacheManager.getCache(name).getAdvancedCache()
				.withFlags(Flag.IGNORE_RETURN_VALUES);
	}

	private final static class ConstantGrouper implements Grouper<Long> {
		@Override
		public String computeGroup(Long key, String group) {
			return "long";
		}

		@Override
		public Class<Long> getKeyType() {
			return Long.class;
		}
	}

	private final static class SingleThreadedExecutorFactory implements
			ExecutorFactory {
		@Override
		public ExecutorService getExecutor(Properties p) {
			return Executors.newSingleThreadExecutor();
		}
	}
}
