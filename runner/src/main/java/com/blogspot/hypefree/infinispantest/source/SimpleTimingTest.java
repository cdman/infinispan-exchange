package com.blogspot.hypefree.infinispantest.source;

import java.util.*;

import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Transaction;
import com.blogspot.hypefree.infinispantest.orderbook.*;

public final class SimpleTimingTest {
	private static long id;
	private static double totalValue;

	public static void main(String[] args) throws Exception {
		List<Order> orders = new ArrayList<>();
		DataSource dataSource = new DataSource();
		while (dataSource.hasNext()) {
			Order order = dataSource.next();
			orders.add(order);
		}
		dataSource.close();
		
		while (true) {
			totalValue = id = 0;
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
			
			System.out.println("" + id + " " + totalValue + " " + duration);

			dataSource = null;
			orderbook = null;
			orderMatcher = null;
			System.gc();
			Thread.sleep(500);
		}
	}
}
