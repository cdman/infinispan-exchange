package com.blogspot.hypefree.infinispantest.orderbook;

import com.blogspot.hypefree.infinispantest.Transaction;

public interface TransactionSink {
	void notifyTransaction(Transaction transaction);
}
