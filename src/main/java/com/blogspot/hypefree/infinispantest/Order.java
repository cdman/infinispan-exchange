package com.blogspot.hypefree.infinispantest;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import org.infinispan.marshall.AdvancedExternalizer;

public final class Order {
	private static final int VERSION = 1;

	private final long id;
	private final Side side;
	private final Market market;
	private final BigDecimal quantity;
	private final BigDecimal filledQuantity;
	private final BigDecimal openQuantity;
	private final BigDecimal price;

	public Order(long id, Side side, Market market, BigDecimal quantity,
			BigDecimal filledQuantity, BigDecimal price) {
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
		this.filledQuantity = filledQuantity;
		this.openQuantity = quantity.subtract(filledQuantity);
		this.price = price;
	}

	public Side getSide() {
		return side;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public long getId() {
		return id;
	}

	public Market getMarket() {
		return market;
	}

	public BigDecimal getFilledQuantity() {
		return filledQuantity;
	}

	public BigDecimal getOpenQuantity() {
		return openQuantity;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Order)) {
			return false;
		}
		Order that = (Order) o;

		return this.id == that.id && this.side.equals(that.side)
				&& this.market.equals(that.market)
				&& this.quantity.equals(that.quantity)
				&& this.filledQuantity.equals(that.filledQuantity)
				&& this.openQuantity.equals(that.openQuantity)
				&& this.price.equals(that.price);
	}

	@Override
	public int hashCode() {
		return (int) id;
	}

	@Override
	public String toString() {
		return String.format("%s %.04f@%.04f", getSide(), getQuantity()
				.doubleValue(), getPrice().doubleValue());
	}

	public static class OrderExternalizer implements
			AdvancedExternalizer<Order> {
		private static final long serialVersionUID = 6643927691200939401L;
		private static final Set<Class<? extends Order>> TYPE_CLASSES = Collections
				.<Class<? extends Order>> singleton(Order.class);
		private static final Integer SERIALIZER_ID = 1;

		@Override
		public void writeObject(ObjectOutput output, Order order)
				throws IOException {
			output.writeInt(VERSION);

			output.writeLong(order.getId());
			output.writeInt(order.getSide().ordinal());
			output.writeInt(order.getMarket().getId());
			output.writeObject(order.getQuantity());
			output.writeObject(order.getFilledQuantity());
			output.writeObject(order.getPrice());
		}

		@Override
		public Order readObject(ObjectInput input) throws IOException,
				ClassNotFoundException {
			int version = input.readInt();
			if (version != VERSION) {
				throw new IllegalArgumentException(
						"Don't know how to deserialized version " + version
								+ " of Order");
			}

			long id = input.readLong();
			Side side = Side.values()[input.readInt()];
			Market market = Market.getById(input.readInt());
			BigDecimal quantity = (BigDecimal) input.readObject();
			BigDecimal filledQuantity = (BigDecimal) input.readObject();
			BigDecimal price = (BigDecimal) input.readObject();

			return new Order(id, side, market, quantity, filledQuantity, price);
		}

		@Override
		public Set<Class<? extends Order>> getTypeClasses() {
			return TYPE_CLASSES;
		}

		@Override
		public Integer getId() {
			return SERIALIZER_ID;
		}
	}
}
