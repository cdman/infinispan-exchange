package com.blogspot.hypefree.infinispantest.runner;

import java.util.*;

import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Transaction;
import com.blogspot.hypefree.infinispantest.orderbook.*;
import com.blogspot.hypefree.infinispantest.source.DataSource;

public final class SimpleTimingTest {
	private static long id;
	private static double totalValue;
	private static double totalQuatity;

	public static void main(String[] args) throws Exception {
		List<Order> orders = new ArrayList<>();
		DataSource dataSource = new DataSource();
		while (dataSource.hasNext()) {
			Order order = dataSource.next();
			orders.add(order);
		}
		dataSource.close();

		while (true) {
			totalValue = totalQuatity = id = 0;
			Orderbook orderbook = new Orderbook();
			OrderMatcher orderMatcher = new OrderMatcher(orderbook,
					new IdSource() {
						@Override
						public long getNextId() {
							return ++id;
						}
					}, new TransactionSink() {
						@Override
						public void notifyTransaction(Transaction transaction) {
							totalQuatity += transaction.getQuantity()
									.doubleValue();
							totalValue += transaction.getQuantity()
									.multiply(transaction.getPrice())
									.doubleValue();
						}
					});

			long start = System.nanoTime();
			for (Order order : orders) {
				orderbook.addOrder(order);
				orderMatcher.runMatching();
			}
			long duration = (System.nanoTime() - start) / 1_000_000;

			totalValue /= 2.0d;
			totalQuatity /= 2.0d;
			System.out.println(String.format("%d\t%.2f\t%.2f\t%d", id,
					totalValue, totalQuatity, duration));

			System.exit(0);

			dataSource = null;
			orderbook = null;
			orderMatcher = null;
			System.gc();
			Thread.sleep(500);
		}
	}
}
