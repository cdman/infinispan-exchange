package com.blogspot.hypefree.infinispantest;

public enum Market {
	BTCUSD(1);
	
	private final int id;
	
	Market(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
}
