package com.blogspot.hypefree.infinispantest;

import java.util.*;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.AdvancedExternalizer;

public final class Constants {
	// don't use strings directly to avoid inlining
	private final static String ORDERBOOKS = "orderbooks";
	private final static String TRANSACTIONS = "transactions";

	public static String getOrderbooksCacheName() {
		return ORDERBOOKS;
	}

	public static String getTransactions() {
		return TRANSACTIONS;
	}

	public static EmbeddedCacheManager createCacheManager() {
		return createCacheManager(Collections
				.<AdvancedExternalizer<?>> emptySet());
	}

	public static EmbeddedCacheManager createCacheManager(
			Collection<AdvancedExternalizer<?>> externalizers) {
		// https://docs.jboss.org/author/display/ISPN/Getting+Started+Guide+-+Clustered+Cache+in+Java+SE
		System.setProperty("jgroups.bind_addr", "127.0.0.1");
		System.setProperty("java.net.preferIPv4Stack", "true");

		Set<AdvancedExternalizer<?>> allExternalizers = new HashSet<>(
				externalizers);
		allExternalizers.add(new Order.OrderExternalizer());
		@SuppressWarnings("unchecked")
		AdvancedExternalizer<Object>[] allExternalizersArray = allExternalizers
				.toArray(new AdvancedExternalizer[allExternalizers.size()]);

		return new DefaultCacheManager(GlobalConfigurationBuilder
				.defaultClusteredBuilder().serialization()
				.addAdvancedExternalizer(allExternalizersArray).transport()
				.build(), new ConfigurationBuilder().invocationBatching()
				.enable().clustering().cacheMode(CacheMode.DIST_SYNC).hash()
				.numOwners(2).build());
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Cache<K, V> getCache(
			EmbeddedCacheManager cacheManager, String name) {
		return (Cache<K, V>) cacheManager.getCache(name).getAdvancedCache()
				.withFlags(Flag.IGNORE_RETURN_VALUES);
	}
}
