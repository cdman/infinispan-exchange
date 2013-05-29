package com.blogspot.hypefree.infinispantest.runner;

import java.math.BigDecimal;
import java.util.*;

import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Transaction;
import com.blogspot.hypefree.infinispantest.orderbook.*;
import com.blogspot.hypefree.infinispantest.source.DataSource;

public final class SimpleTimingTest {
	private static long id;
	private static double totalValue;
	private static double totalQuatity;
	private static BigDecimal totalValueBig;
	private static BigDecimal totalQuatityBig;

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
			totalValueBig = totalQuatityBig = BigDecimal.ZERO;
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
							totalQuatityBig = totalQuatityBig.add(transaction
									.getQuantity());
							totalValueBig = totalValueBig.add(transaction
									.getQuantity().multiply(
											transaction.getPrice()));
						}
					});

			long start = System.nanoTime();
			for (Order order : orders) {
				orderbook.addOrder(order);
				orderMatcher.runMatching();
			}
			long duration = (System.nanoTime() - start) / 1_000_000;

			totalValue /= 2.0d;
			totalValueBig = totalValueBig.divide(BigDecimal.valueOf(2));
			totalQuatity /= 2.0d;
			totalQuatityBig = totalQuatityBig.divide(BigDecimal.valueOf(2));
			
			System.out.println(String.format("%d\t%.8f\t%.8f\t%d", id,
					totalValue, totalQuatity, duration));
			System.out.println(totalQuatityBig.subtract(BigDecimal.valueOf(totalQuatity)));
			System.out.println(totalValueBig.subtract(BigDecimal.valueOf(totalValue)));
			System.out.println();

			dataSource = null;
			orderbook = null;
			orderMatcher = null;
			System.gc();
			Thread.sleep(500);
		}
	}
}
