package com.blogspot.hypefree.infinispantest.venue;

import java.util.*;

import org.apache.commons.lang3.math.Fraction;

import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Side;
import com.blogspot.hypefree.infinispantest.Transaction;

final class OrderMatcher {
	private static final Fraction TWO = Fraction.getFraction(2, 1);

	private final SortedOrdersHolder buyOrders, sellOrders;
	private final Map<Long, Order> orderSource;
	private final IdSource transactionIdSource;
	private final TransactionSink transactionSink;

	OrderMatcher(Map<Long, Order> orderSource, IdSource transactionIdSource,
			TransactionSink transactionSink) {
		this.orderSource = orderSource;
		this.transactionIdSource = transactionIdSource;
		this.transactionSink = transactionSink;
		this.buyOrders = new SortedOrdersHolder();
		this.sellOrders = new SortedOrdersHolder();
	}

	void notifyOrderAdded(Order order) {
		notifyExistingOrder(order);

		matchLoop: while (true) {
			if (buyOrders.isEmpty() || sellOrders.isEmpty()) {
				break;
			}

			Fraction bestBuyPrice = buyOrders.getHighestPrice();
			for (Long buyOrderId : buyOrders.getOrderIdsAtPrice(bestBuyPrice)) {
				Order buyOrder = orderSource.get(buyOrderId);
				assert buyOrder.getPrice().equals(bestBuyPrice);

				Fraction matchingSellPrice = sellOrders
						.getPriceLowerOrEqual(buyOrder.getPrice());
				if (matchingSellPrice == null) {
					break matchLoop;
				}

				Fraction matchPrice = bestBuyPrice.add(matchingSellPrice)
						.divideBy(TWO);
				for (Long sellOrderId : sellOrders
						.getOrderIdsAtPrice(matchingSellPrice)) {
					Order sellOrder = orderSource.get(sellOrderId);
					assert sellOrder.getPrice().equals(matchingSellPrice);

					Fraction matchQuantity;
					if (buyOrder.getQuantity().compareTo(
							sellOrder.getQuantity()) > 0) {
						matchQuantity = sellOrder.getQuantity();
					} else {
						matchQuantity = buyOrder.getQuantity();
					}

					long currentTime = System.currentTimeMillis();
					transactionSink.notifyTransaction(new Transaction(
							transactionIdSource.getNextId(), Side.BUY,
							buyOrderId, sellOrderId, matchQuantity, matchPrice,
							currentTime));
					transactionSink.notifyTransaction(new Transaction(
							transactionIdSource.getNextId(), Side.SELL,
							sellOrderId, buyOrderId, matchQuantity, matchPrice,
							currentTime));

					buyOrder = decreaseOrder(buyOrders, buyOrder, matchQuantity);
					decreaseOrder(sellOrders, sellOrder, matchQuantity);
					if (buyOrder.getOpenQuantity().equals(Fraction.ZERO)) {
						break;
					}
				}
			}

		}
	}

	private Order decreaseOrder(SortedOrdersHolder ordersHolder, Order order,
			Fraction quantityDelta) {
		Order result = new Order(order.getId(), order.getSide(),
				order.getMarket(), order.getQuantity(), order
						.getFilledQuantity().add(quantityDelta),
				order.getPrice());
		if (result.getOpenQuantity().equals(Fraction.ZERO)) {
			ordersHolder.removeOrderId(order.getPrice(), order.getId());
			transactionSink.notifyOrderCompletelyFilled(order);
		} else {
			transactionSink.notifyOrderPartiallyFilled(order);
		}
		return result;
	}

	void clear() {
		buyOrders.clear();
		sellOrders.clear();
	}

	void notifyExistingOrder(Order order) {
		assert order != null;

		switch (order.getSide()) {
		case BUY:
			buyOrders.addOrderId(order.getPrice(), order.getId());
			break;
		case SELL:
			sellOrders.addOrderId(order.getPrice(), order.getId());
			break;
		default:
			assert false;
		}
	}
}
