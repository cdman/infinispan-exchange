package com.blogspot.hypefree.infinispantest;

import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.TransactionProtocol;
import org.infinispan.transaction.lookup.GenericTransactionManagerLookup;
import org.infinispan.util.concurrent.IsolationLevel;

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
		// https://docs.jboss.org/author/display/ISPN/Getting+Started+Guide+-+Clustered+Cache+in+Java+SE
		System.setProperty("jgroups.bind_addr", "127.0.0.1");
		System.setProperty("java.net.preferIPv4Stack", "true");

		return new DefaultCacheManager(GlobalConfigurationBuilder
				.defaultClusteredBuilder().serialization().transport().build(),
				new ConfigurationBuilder()
						.transaction()
							.transactionMode(TransactionMode.TRANSACTIONAL)
							.transactionManagerLookup(new GenericTransactionManagerLookup())
							.transactionProtocol(TransactionProtocol.DEFAULT)
							.lockingMode(LockingMode.PESSIMISTIC)
							.recovery().enable()
						.locking()
							.concurrencyLevel(10)
						.useLockStriping(false)
						.isolationLevel(IsolationLevel.READ_COMMITTED)
						.clustering()
							.cacheMode(CacheMode.DIST_SYNC)
							.hash()
							.numOwners(2)
							.sync()
							.replTimeout(5, TimeUnit.SECONDS)
							.stateTransfer()
								.fetchInMemoryState(true)
						.build());
	}

	@SuppressWarnings("unchecked")
	public static <K, V> Cache<K, V> getCache(
			EmbeddedCacheManager cacheManager, String name) {
		return (Cache<K, V>) cacheManager.getCache(name).getAdvancedCache()
				.withFlags(Flag.IGNORE_RETURN_VALUES);
	}
}
