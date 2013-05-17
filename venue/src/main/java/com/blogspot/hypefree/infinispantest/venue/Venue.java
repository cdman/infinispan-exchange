package com.blogspot.hypefree.infinispantest.venue;

import java.util.concurrent.atomic.AtomicLong;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import com.blogspot.hypefree.infinispantest.Constants;
import com.blogspot.hypefree.infinispantest.HotRodMapWrapper;
import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Transaction;

public final class Venue {
	private static final Log LOG = LogFactory.getLog(Venue.class);

	private final EmbeddedCacheManager cacheManager;
	private final OrderMatcher orderMatcher;
	private final Cache<byte[], byte[]> orders;
	private final HotRodMapWrapper<Long, Order> ordersWrapper;
	private final Cache<byte[], byte[]> transactions;
	private final HotRodMapWrapper<Long, Transaction> transactionsWrapper;

	private volatile boolean isPrimary;

	public Venue() {
		cacheManager = Constants.createCacheManager();

		orders = Constants.getCache(cacheManager,
				Constants.getOrdersCacheName());
		ordersWrapper = HotRodMapWrapper.wrap(orders);
		transactions = Constants.getCache(cacheManager,
				Constants.getTransactions());
		transactionsWrapper = HotRodMapWrapper.wrap(transactions);
		orderMatcher = new OrderMatcher(ordersWrapper,
				new SpeculativeTransactionIdSource(cacheManager.hashCode()),
				new VenueTransactionSink());

		orders.addListener(new OrderAddedListener());
		cacheManager.addListener(new TopologyChangeListener());
	}

	public void run() {

	}

	@Listener(sync = false)
	public final class TopologyChangeListener {
		private HotRodServer hotRodServer;

		private void setPrimary() {
			isPrimary = true;
			LOG.info("Became primary node!");

			orderMatcher.clear();
			for (byte[] ordersSerialized : orders.values()) {
				Order order = ordersWrapper.deserialize(ordersSerialized);
				orderMatcher.notifyExistingOrder(order);
			}

			hotRodServer = new HotRodServer();
			hotRodServer.start(new HotRodServerConfigurationBuilder().build(),
					cacheManager);
		}

		private void removePrimary() {
			isPrimary = false;
			LOG.info("Lost primary node status!");

			orderMatcher.clear();

			hotRodServer.stop();
			hotRodServer = null;
		}

		@ViewChanged
		public void viewChanged(ViewChangedEvent viewChangeEvent) {
			Address primaryNode = orders.getAdvancedCache()
					.getDistributionManager().getConsistentHash()
					.locatePrimaryOwner(ordersWrapper.serialize(1L));
			if (!primaryNode.equals(viewChangeEvent.getLocalAddress())) {
				if (isPrimary) {
					removePrimary();
				}
			} else {
				if (!isPrimary) {
					setPrimary();
				}
			}
		}
	}

	@Listener(sync = false)
	public final class OrderAddedListener {
		@CacheEntryModified
		public void cacheEntryCreated(
				CacheEntryModifiedEvent<byte[], byte[]> event) {
			if (event.isPre() || !event.isCreated() || !isPrimary) {
				return;
			}
			Order order = ordersWrapper.deserialize(event.getValue());

			orders.startBatch();
			transactions.startBatch();
			boolean success = false;
			try {
				orderMatcher.notifyOrderAdded(order);
				success = true;
			} finally {
				orders.endBatch(success);
				transactions.endBatch(success);
			}
		}
	}

	private final class SpeculativeTransactionIdSource implements IdSource {
		private final long uniqIdHi;
		private final AtomicLong uniqIdLow;

		SpeculativeTransactionIdSource(int seed) {
			this.uniqIdHi = (long) seed << 31L + System.currentTimeMillis() << 16L;
			this.uniqIdLow = new AtomicLong();
		}

		@Override
		public Long getNextId() {
			return uniqIdLow.incrementAndGet() + uniqIdHi;
		}
	}

	private final class VenueTransactionSink implements TransactionSink {
		@Override
		public void notifyTransaction(Transaction transaction) {
			transactionsWrapper.put(transaction.getId(), transaction);
		}

		@Override
		public void notifyOrderCompletelyFilled(Order order) {
			ordersWrapper.remove(order.getId());
		}

		@Override
		public void notifyOrderPartiallyFilled(Order order) {
			ordersWrapper.put(order.getId(), order);
		}
	}
}
