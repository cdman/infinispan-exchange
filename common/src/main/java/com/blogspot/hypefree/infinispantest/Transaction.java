package com.blogspot.hypefree.infinispantest;

import java.io.Serializable;

import org.apache.commons.lang3.math.Fraction;

public final class Transaction implements Serializable {
	private static final long serialVersionUID = -62767919488695708L;

	private final Long id;
	private final Side side;
	private final Long sourceOrderId;
	private final Long destinationOrderId;
	private final Fraction quantity;
	private final Fraction price;
	private final long timestamp;

	public Transaction(Long id, Side side, Long sourceOrderId,
			Long destinationOrderId, Fraction quantity, Fraction price,
			long timestamp) {
		this.id = id;
		this.side = side;
		this.sourceOrderId = sourceOrderId;
		this.destinationOrderId = destinationOrderId;
		this.quantity = quantity;
		this.price = price;
		this.timestamp = timestamp;
	}

	public Long getId() {
		return id;
	}

	public Side getSide() {
		return side;
	}

	public Long getSourceOrderId() {
		return sourceOrderId;
	}

	public Long getDestinationOrderId() {
		return destinationOrderId;
	}

	public Fraction getQuantity() {
		return quantity;
	}

	public Fraction getPrice() {
		return price;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Transaction)) {
			return false;
		}
		Transaction that = (Transaction) o;

		return this.id.equals(that.id) && this.side.equals(that.side)
				&& this.sourceOrderId.equals(that.sourceOrderId)
				&& this.destinationOrderId.equals(that.destinationOrderId)
				&& this.quantity.equals(that.quantity)
				&& this.price.equals(that.price);
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
}
