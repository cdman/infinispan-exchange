package com.blogspot.hypefree.infinispantest.orderbook;

import java.math.BigDecimal;
import java.util.*;
import org.junit.*;

import static org.junit.Assert.*;

import com.blogspot.hypefree.infinispantest.Market;
import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Side;
import com.blogspot.hypefree.infinispantest.Transaction;
import com.blogspot.hypefree.infinispantest.orderbook.IdSource;
import com.blogspot.hypefree.infinispantest.orderbook.OrderMatcher;
import com.blogspot.hypefree.infinispantest.orderbook.TransactionSink;

public final class OrderMatcherTest {
	private Orderbook orderbook;
	private List<Transaction> transactions;
	private OrderMatcher orderMatcher;

	@Test
	public void simpleTest() {
		orderbook.addOrder(new Order(1L, Side.BUY, Market.BTCUSD,
				BigDecimal.valueOf(10L), BigDecimal.ZERO,
				BigDecimal.valueOf(5L)));
		orderbook.addOrder(new Order(2L, Side.SELL, Market.BTCUSD,
				BigDecimal.valueOf(5L), BigDecimal.ZERO, BigDecimal.valueOf(1L)));
		orderMatcher.runMatching();

		assertEquals(Arrays.asList(new Transaction(1L, Side.BUY, 1L, 2L,
				BigDecimal.valueOf(5L), BigDecimal.valueOf(3L), 0),
				new Transaction(2L, Side.SELL, 2L, 1L, BigDecimal.valueOf(5L),
						BigDecimal.valueOf(3L), 0)), transactions);

		// filled orders are removed
		assertTrue(orderbook.hasBuyOrder());
		assertFalse(orderbook.hasSellOrder());
	}

	@Before
	public void setUp() {
		orderbook = new Orderbook();
		transactions = new ArrayList<>();
		orderMatcher = new OrderMatcher(orderbook, new IdSource() {
			private long counter = 0;

			@Override
			public long getNextId() {
				return ++counter;
			}
		}, new TransactionSink() {
			@Override
			public void notifyTransaction(Transaction transaction) {
				transactions.add(transaction);
			}
		});
	}
}
