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

	public static Market getById(int id) {
		for (Market m : Market.values()) {
			if (m.getId() == id) {
				return m;
			}
		}
		throw new IllegalArgumentException("Market with id " + id + " does not exists!");
	}
}
