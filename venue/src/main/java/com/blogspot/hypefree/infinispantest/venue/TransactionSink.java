package com.blogspot.hypefree.infinispantest.venue;

import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Transaction;

interface TransactionSink {
	void notifyTransaction(Transaction transaction);

	void notifyOrderCompletelyFilled(Order order);

	void notifyOrderPartiallyFilled(Order order);
}
