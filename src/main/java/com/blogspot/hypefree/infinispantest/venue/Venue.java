package com.blogspot.hypefree.infinispantest.venue;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.TransactionManager;

import org.infinispan.Cache;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;
import org.infinispan.distribution.ch.ConsistentHash;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import com.blogspot.hypefree.infinispantest.Constants;
import com.blogspot.hypefree.infinispantest.Market;
import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Transaction;
import com.blogspot.hypefree.infinispantest.orderbook.IdSource;
import com.blogspot.hypefree.infinispantest.orderbook.OrderMatcher;
import com.blogspot.hypefree.infinispantest.orderbook.Orderbook;
import com.blogspot.hypefree.infinispantest.orderbook.TransactionSink;
import com.blogspot.hypefree.infinispantest.source.DataSource;

public final class Venue {
	private static final Log LOG = LogFactory.getLog(Venue.class);

	private static volatile Venue INSTANCE;

	private final EmbeddedCacheManager cacheManager;
	private final Cache<String, Orderbook> orderbooks;
	private final Cache<Long, Transaction> transactions;
	private final ExecutorService eventProcessorThread;
	private final EventProcessor eventProcessor;
	private final GrizzlyServer grizzlyServer;

	public Venue() {
		cacheManager = Constants.createCacheManager();

		orderbooks = Constants.getCache(cacheManager,
				Constants.getOrderbooksCacheName());
		transactions = Constants.getCache(cacheManager,
				Constants.getTransactions());

		eventProcessor = new EventProcessor(cacheManager.getAddress()
				.hashCode());
		eventProcessorThread = Executors.newSingleThreadExecutor();

		eventProcessorThread.submit(new Runnable() {
			@Override
			public void run() {
				eventProcessor.updateTopology();
			}
		});

		orderbooks.addListener(new TopologyChangeListener());

		grizzlyServer = new GrizzlyServer();

		Venue.INSTANCE = this;
	}

	public void run() {
		Thread loader = new Thread(new Runnable() {
			@Override
			public void run() {
				for (int i = 20; i > 0; --i) {
					System.out.println("Waiting for startup " + i);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				ConsistentHash hash = orderbooks.getAdvancedCache()
						.getDistributionManager().getConsistentHash();
				Address localAddress = cacheManager.getAddress();
				Address primaryNode = hash.locatePrimaryOwner(Market.BTCUSD
						.name());
				if (!localAddress.equals(primaryNode)) {
					return;
				}

				List<Order> orders = new ArrayList<>();
				try {
					DataSource dataSource = new DataSource();
					while (dataSource.hasNext()) {
						Order order = dataSource.next();
						orders.add(order);
					}
					dataSource.close();
				} catch (Exception ex) {
					ex.printStackTrace();
					return;
				}

				long start = System.nanoTime();
				int index = 0;
				for (Order order : orders) {
					Future<?> future = addOrder(order);
					if (++index % 1_000 == 0) {
						try {
							future.get();
						} catch (InterruptedException | ExecutionException e1) {
							e1.printStackTrace();
						}
						System.out.println(index);
					}
				}
				long duration = (System.nanoTime() - start) / 1_000_000;

				System.out.println("Total traded volume: " + getTradedVolume());
				System.out.println(duration / 1_000.0d);
			}
		});

		if (System.getProperties().containsKey("localLoad")) {
			loader.start();
		}

		grizzlyServer.start();
	}

	static Venue getInstance() {
		return INSTANCE;
	}

	@Listener
	public final class TopologyChangeListener {
		@TopologyChanged
		public void viewChanged(
				TopologyChangedEvent<Object, Object> topologyChangedEvent) {
			if (topologyChangedEvent.isPre()) {
				return;
			}
			eventProcessorThread.submit(new Runnable() {
				@Override
				public void run() {
					eventProcessor.updateTopology();
				}
			});
		}
	}

	Future<?> addOrder(final Order order) {
		return eventProcessorThread.submit(new Runnable() {
			@Override
			public void run() {
				eventProcessor.addOrder(order);
			}
		});
	}

	double getTradedVolume() {
		MapReduceTask<Long, Transaction, Long, Double> mrTask = new MapReduceTask<Long, Transaction, Long, Double>(
				transactions);
		mrTask.mappedWith(new TradedVolumeMapper()).reducedWith(
				new TradedVolumeReducer());

		Double result = mrTask.execute().get(0L);
		if (result == null) {
			result = 0.0d;
		}
		return result / 2.0d;
	}

	private final static class TradedVolumeMapper implements
			Mapper<Long, Transaction, Long, Double> {
		private static final long serialVersionUID = 8659000998496309022L;

		@Override
		public void map(Long key, Transaction value,
				Collector<Long, Double> collector) {
			collector.emit(0L, value.getQuantity().doubleValue());
		}
	}

	private final static class TradedVolumeReducer implements
			Reducer<Long, Double> {
		private static final long serialVersionUID = 8302490513774886177L;

		@Override
		public Double reduce(Long reducedKey, Iterator<Double> iter) {
			double sum = 0;
			while (iter.hasNext()) {
				Double i = iter.next();
				sum += i;
			}
			return sum;
		}
	}

	private final class EventProcessor {
		private final Set<String> primaryMarkets;
		private final Map<String, OrderMatcher> orderMatchers;
		private final IdSource idSource;
		private final TransactionSink transactionSink;
		private final TransactionManager tm;

		EventProcessor(int seed) {
			primaryMarkets = new HashSet<>();
			orderMatchers = new HashMap<>();
			idSource = new SpeculativeTransactionIdSource(seed);
			transactionSink = new VenueTransactionSink();
			tm = orderbooks.getAdvancedCache().getTransactionManager();
		}

		void updateTopology() {
			LOG.info("Was primary for " + primaryMarkets);
			primaryMarkets.clear();

			ConsistentHash hash = orderbooks.getAdvancedCache()
					.getDistributionManager().getConsistentHash();
			Address localAddress = cacheManager.getAddress();
			for (Market market : Market.values()) {
				Address primaryNode = hash.locatePrimaryOwner(market.name());
				if (localAddress.equals(primaryNode)) {
					primaryMarkets.add(market.name());
					orderbooks.putIfAbsent(market.name(), new Orderbook());
				}
			}
			LOG.info("Is primary for " + primaryMarkets);

			orderMatchers.clear();
			for (String marketName : primaryMarkets) {
				orderMatchers.put(marketName,
						new OrderMatcher(orderbooks.get(marketName), idSource,
								transactionSink));
			}
		}

		void addOrder(Order order) {
			String marketName = order.getMarket().toString();
			if (!primaryMarkets.contains(marketName)) {
				throw new IllegalArgumentException(
						"This node is not the primary for " + marketName
								+ " (Order: " + order + ")");
			}

			try {
				tm.begin();
				try {
					Orderbook orderbook = orderbooks.get(marketName);
					orderbook.addOrder(order);
					orderMatchers.get(marketName).runMatching();
					orderbooks.put(marketName, orderbook);
					tm.commit();
				} catch (Exception e) {
					LOG.error("Error adding Order " + order
							+ ", will roll back.", e);
					tm.rollback();
				}
			} catch (Exception e) {
				LOG.error("Transaction failure for Order " + order, e);
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
		public long getNextId() {
			return uniqIdLow.incrementAndGet() + uniqIdHi;
		}
	}

	private final class VenueTransactionSink implements TransactionSink {
		@Override
		public void notifyTransaction(Transaction transaction) {
			transactions.put(transaction.getId(), transaction);
		}
	}
}
