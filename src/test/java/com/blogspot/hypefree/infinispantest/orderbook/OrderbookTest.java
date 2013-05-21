package com.blogspot.hypefree.infinispantest.orderbook;

import java.math.BigDecimal;

import org.junit.*;
import static org.junit.Assert.*;

import com.blogspot.hypefree.infinispantest.Market;
import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Side;

public final class OrderbookTest {
	private final static BigDecimal PRICE = BigDecimal.valueOf(5L);
	private long orderId;
	private Orderbook orderbook;

	@Test
	public void testDeltas() {
		Order buyOrder1 = getBuyOrder(5), buyOrder2 = getBuyOrder(3), sellOrder = getSellOrder(7);
		orderbook.addOrder(buyOrder1);
		orderbook.addOrder(buyOrder2);
		orderbook.addOrder(sellOrder);

		orderbook.fill(buyOrder1, BigDecimal.valueOf(5L));
		orderbook.fill(buyOrder2, BigDecimal.valueOf(2L));
		orderbook.fill(sellOrder, BigDecimal.valueOf(5L));

		Orderbook newOrderbook = (Orderbook) orderbook.delta().merge(null);
		assertEquals(orderbook, newOrderbook);
		assertEquals(2, orderbook.getActiveOrderCount());
	}

	private Order getBuyOrder(long quantity) {
		return new Order(++orderId, Side.BUY, Market.BTCUSD,
				BigDecimal.valueOf(quantity), BigDecimal.ZERO, PRICE);
	}

	private Order getSellOrder(long quantity) {
		return new Order(++orderId, Side.SELL, Market.BTCUSD,
				BigDecimal.valueOf(quantity), BigDecimal.ZERO, PRICE);
	}

	@Before
	public void setUp() {
		orderId = 0;
		orderbook = new Orderbook();
	}
}
