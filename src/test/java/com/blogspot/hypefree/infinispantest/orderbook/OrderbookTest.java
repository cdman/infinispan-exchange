package com.blogspot.hypefree.infinispantest.orderbook;

import java.math.BigDecimal;
import java.util.Arrays;

import org.infinispan.marshall.jboss.JBossMarshaller;
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

	@Test
	public void testDeSerializaton() throws Exception {
		orderbook.addOrder(getBuyOrder(5));
		orderbook.addOrder(getBuyOrder(3));
		orderbook.addOrder(getSellOrder(7));
		orderbook.commit();

		JBossMarshaller marshaller = new JBossMarshaller();
		Orderbook deserialized = (Orderbook) marshaller
				.objectFromByteBuffer(marshaller.objectToByteBuffer(orderbook));
		assertEquals(3, deserialized.getActiveOrderCount());
	}

	@Test
	public void testOrderOderAfterDeSerializaton() throws Exception {
		Order buyOrder1 = getBuyOrder(5), buyOrder2 = getBuyOrder(3);
		orderbook.addOrder(buyOrder1);
		orderbook.addOrder(buyOrder2);
		orderbook.commit();

		JBossMarshaller marshaller = new JBossMarshaller();
		Orderbook deserialized = (Orderbook) marshaller
				.objectFromByteBuffer(marshaller.objectToByteBuffer(orderbook));

		assertEquals(Arrays.asList(buyOrder1, buyOrder2),
				deserialized.getOrders(Side.BUY, PRICE));
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
