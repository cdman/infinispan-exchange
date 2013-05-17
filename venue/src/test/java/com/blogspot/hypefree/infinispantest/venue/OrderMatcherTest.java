package com.blogspot.hypefree.infinispantest.venue;

import java.util.*;
import org.apache.commons.lang3.math.Fraction;
import org.junit.*;

import static org.junit.Assert.*;

import com.blogspot.hypefree.infinispantest.Market;
import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Side;
import com.blogspot.hypefree.infinispantest.Transaction;

public final class OrderMatcherTest {
	private Map<Long, Order> orders;
	private List<Transaction> transactions;
	private OrderMatcher orderMatcher;

	@Test
	public void simpleTest() {
		Order order1 = new Order(1L, Side.BUY, Market.BTCUSD,
				Fraction.getFraction(10), Fraction.ZERO,
				Fraction.getFraction(5));
		orders.put(1L, order1);
		orderMatcher.notifyOrderAdded(order1);
		Order order2 = new Order(2L, Side.SELL, Market.BTCUSD,
				Fraction.getFraction(5), Fraction.ZERO, Fraction.getFraction(1));
		orders.put(2L, order2);
		orderMatcher.notifyOrderAdded(order2);

		assertEquals(Arrays.asList(new Transaction(1L, Side.BUY, 1L, 2L,
				Fraction.getFraction(5), Fraction.getFraction(3), 0),
				new Transaction(2L, Side.SELL, 2L, 1L, Fraction.getFraction(5),
						Fraction.getFraction(3), 0)), transactions);

		// filled orders are removed
		assertEquals(1, orders.size());
	}

	@Before
	public void setUp() {
		orders = new HashMap<>();
		transactions = new ArrayList<>();
		orderMatcher = new OrderMatcher(orders, new IdSource() {
			private long counter = 0;

			@Override
			public Long getNextId() {
				return ++counter;
			}
		}, new TransactionSink() {
			@Override
			public void notifyTransaction(Transaction transaction) {
				transactions.add(transaction);
			}

			@Override
			public void notifyOrderCompletelyFilled(Order order) {
				orders.remove(order.getId());
			}

			@Override
			public void notifyOrderPartiallyFilled(Order order) {
				orders.put(order.getId(), order);
			}
		});
	}
}
