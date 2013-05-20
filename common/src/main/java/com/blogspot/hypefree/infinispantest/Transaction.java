package com.blogspot.hypefree.infinispantest;

import java.io.Serializable;
import java.math.BigDecimal;

public final class Transaction implements Serializable {
	private static final long serialVersionUID = -62767919488695708L;

	private final long id;
	private final Side side;
	private final Long sourceOrderId;
	private final Long destinationOrderId;
	private final BigDecimal quantity;
	private final BigDecimal price;
	private final long timestamp;

	public Transaction(long id, Side side, Long sourceOrderId,
			Long destinationOrderId, BigDecimal quantity, BigDecimal price,
			long timestamp) {
		this.id = id;
		this.side = side;
		this.sourceOrderId = sourceOrderId;
		this.destinationOrderId = destinationOrderId;
		this.quantity = quantity;
		this.price = price;
		this.timestamp = timestamp;
	}

	public long getId() {
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

	public BigDecimal getQuantity() {
		return quantity;
	}

	public BigDecimal getPrice() {
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

		return this.id == that.id && this.side.equals(that.side)
				&& this.sourceOrderId.equals(that.sourceOrderId)
				&& this.destinationOrderId.equals(that.destinationOrderId)
				&& this.quantity.equals(that.quantity)
				&& this.price.equals(that.price);
	}
	
	@Override
	public int hashCode() {
		return (int)id;
	}
}
