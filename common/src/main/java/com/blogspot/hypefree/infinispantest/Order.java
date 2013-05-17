package com.blogspot.hypefree.infinispantest;

import java.io.Serializable;

import org.apache.commons.lang3.math.Fraction;

public final class Order implements Serializable {
	private static final long serialVersionUID = 5682426217902798243L;

	private final Long id;
	private final Side side;
	private final Market market;
	private final Fraction quantity;
	private final Fraction price;
	private final Fraction filledQuantity;
	private final Fraction openQuantity;

	public Order(Long id, Side side, Market market, Fraction quantity, Fraction filledQuantity,
			Fraction price) {
		assert side != null;
		assert market != null;
		assert quantity != null;
		assert filledQuantity != null;
		assert quantity.compareTo(filledQuantity) >= 0;
		assert price != null;

		this.id = id;
		this.side = side;
		this.market = market;
		this.quantity = quantity;
		this.price = price;
		this.filledQuantity = filledQuantity;
		this.openQuantity = quantity.subtract(filledQuantity);
	}

	public Side getSide() {
		return side;
	}

	public Fraction getQuantity() {
		return quantity;
	}

	public Fraction getPrice() {
		return price;
	}

	public Long getId() {
		return id;
	}

	public Market getMarket() {
		return market;
	}

	public Fraction getFilledQuantity() {
		return filledQuantity;
	}

	public Fraction getOpenQuantity() {
		return openQuantity;
	}
}
