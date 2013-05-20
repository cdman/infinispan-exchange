package com.blogspot.hypefree.infinispantest.orderbook;

import java.math.BigDecimal;

import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Side;
import com.blogspot.hypefree.infinispantest.Transaction;

public final class OrderMatcher {
	private static final BigDecimal TWO = BigDecimal.valueOf(2L);

	private final Orderbook orderbook;
	private final IdSource transactionIdSource;
	private final TransactionSink transactionSink;

	public OrderMatcher(Orderbook orderbook, IdSource transactionIdSource,
			TransactionSink transactionSink) {
		this.orderbook = orderbook;
		this.transactionIdSource = transactionIdSource;
		this.transactionSink = transactionSink;
	}

	public void runMatching() {
		while (true) {
			if (!orderbook.hasSellOrder() || !orderbook.hasBuyOrder()) {
				break;
			}

			BigDecimal bestBuyPrice = orderbook.getBestPrice(Side.BUY);
			for (Order buyOrder : orderbook.getOrders(Side.BUY, bestBuyPrice)) {
				assert buyOrder.getPrice().equals(bestBuyPrice);

				BigDecimal matchingSellPrice = orderbook.getPriceCeiled(
						Side.SELL, bestBuyPrice);
				if (matchingSellPrice == null) {
					return;
				}

				BigDecimal matchPrice = bestBuyPrice.add(matchingSellPrice)
						.divide(TWO);
				for (Order sellOrder : orderbook.getOrders(Side.SELL,
						matchingSellPrice)) {
					assert sellOrder.getPrice().equals(matchingSellPrice);

					BigDecimal matchQuantity;
					if (buyOrder.getOpenQuantity().compareTo(
							sellOrder.getOpenQuantity()) > 0) {
						matchQuantity = sellOrder.getOpenQuantity();
					} else {
						matchQuantity = buyOrder.getOpenQuantity();
					}

					long currentTime = System.currentTimeMillis();

					Order newBuyOrder = orderbook.fill(buyOrder, matchQuantity);
					transactionSink.notifyTransaction(new Transaction(
							transactionIdSource.getNextId(), Side.BUY, buyOrder
									.getId(), sellOrder.getId(), matchQuantity,
							matchPrice, currentTime));

					orderbook.fill(sellOrder, matchQuantity);
					transactionSink.notifyTransaction(new Transaction(
							transactionIdSource.getNextId(), Side.SELL,
							sellOrder.getId(), buyOrder.getId(), matchQuantity,
							matchPrice, currentTime));

					buyOrder = newBuyOrder;
					if (buyOrder == null
							|| buyOrder.getOpenQuantity().equals(
									BigDecimal.ZERO)) {
						break;
					}
				}
			}

		}
	}
}
