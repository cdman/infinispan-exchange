package com.blogspot.hypefree.infinispantest.orderbook;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import org.infinispan.atomic.Delta;
import org.infinispan.atomic.DeltaAware;
import org.infinispan.marshall.Externalizer;
import org.infinispan.marshall.SerializeWith;

import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Side;

@SerializeWith(Orderbook.OrderbookExternalizer.class)
public final class Orderbook implements DeltaAware {
	private final OrderbookDelta delta;
	private final NavigableMap<BigDecimal, List<Order>> buyOrders, sellOrders;

	public Orderbook() {
		delta = new OrderbookDelta(Collections.<OrderbookOperation> emptyList());
		buyOrders = new TreeMap<>();
		sellOrders = new TreeMap<>(Collections.reverseOrder());
	}

	@Override
	public Delta delta() {
		Delta result = delta.getCopy();
		commit();
		return result;
	}

	@Override
	public void commit() {
		delta.clear();
	}

	private NavigableMap<BigDecimal, List<Order>> getMap(Side side) {
		switch (side) {
		case BUY:
			return buyOrders;
		case SELL:
			return sellOrders;
		default:
			throw new IllegalArgumentException("Unhandled order side: " + side);
		}
	}

	public boolean hasSellOrder() {
		return !getMap(Side.SELL).isEmpty();
	}

	public boolean hasBuyOrder() {
		return !getMap(Side.BUY).isEmpty();
	}

	public BigDecimal getBestPrice(Side side) {
		return getMap(side).lastKey();
	}

	public BigDecimal getPriceCeiled(Side side, BigDecimal referencePrice) {
		return getMap(side).ceilingKey(referencePrice);
	}

	public List<Order> getOrders(Side side, BigDecimal price) {
		List<Order> orders = getMap(side).get(price);
		if (orders == null) {
			return Collections.emptyList();
		}
		return new ArrayList<>(orders);
	}

	public void addOrder(Order order) {
		delta.addOperation(this, new AddOperation(order));
	}

	public Order fill(Order order, BigDecimal matchQuantity) {
		delta.addOperation(this, new FillOperation(order, matchQuantity));

		NavigableMap<BigDecimal, List<Order>> map = getMap(order.getSide());
		List<Order> list = map.get(order.getPrice());
		if (list == null || list.isEmpty()
				|| list.get(0).getId() != order.getId()) {
			return null;
		}
		return list.get(0);
	}

	public int getActiveOrderCount() {
		int result = 0;
		for (List<Order> orderList : getMap(Side.BUY).values()) {
			result += orderList.size();
		}
		for (List<Order> orderList : getMap(Side.SELL).values()) {
			result += orderList.size();
		}
		return result;
	}

	public static abstract class OrderbookOperation {
		abstract void apply(Orderbook orderbook);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Orderbook)) {
			return false;
		}
		Orderbook that = (Orderbook) o;
		return this.buyOrders.equals(that.buyOrders)
				&& this.sellOrders.equals(that.sellOrders);
	}

	@Override
	public int hashCode() {
		// chosen by a fair role of dice :-)
		return 9;
	}

	@SerializeWith(Orderbook.AddOperation.AddOperationExternalizer.class)
	public static final class AddOperation extends OrderbookOperation {
		private final static int VERSION = 1;

		private Order order;

		AddOperation(Order order) {
			this.order = order;
		}

		@Override
		void apply(Orderbook orderbook) {
			NavigableMap<BigDecimal, List<Order>> map = orderbook.getMap(order
					.getSide());
			List<Order> list = map.get(order.getPrice());
			if (list == null) {
				list = new ArrayList<>();
				map.put(order.getPrice(), list);
			}
			for (int i = 0; i < list.size(); ++i) {
				if (list.get(i).getId() == order.getId()) {
					return;
				}
			}
			list.add(order);
		}

		public static final class AddOperationExternalizer implements
				Externalizer<AddOperation> {
			private static final long serialVersionUID = 8428067842090985415L;

			@Override
			public void writeObject(ObjectOutput output, AddOperation object)
					throws IOException {
				output.writeInt(VERSION);
				output.writeObject(object.order);
			}

			@Override
			public AddOperation readObject(ObjectInput input)
					throws IOException, ClassNotFoundException {
				int version = input.readInt();
				if (version != VERSION) {
					throw new IllegalArgumentException(
							"Don't know how to read AddOperation version "
									+ version);
				}
				return new AddOperation((Order) input.readObject());
			}
		}
	}

	@SerializeWith(Orderbook.FillOperation.FillOperationExternalizer.class)
	public final static class FillOperation extends OrderbookOperation {
		private final static int VERSION = 1;

		private final Order order;
		private final BigDecimal matchQuantity;

		FillOperation(Order order, BigDecimal matchQuantity) {
			this.order = order;
			this.matchQuantity = matchQuantity;
			if (order.getOpenQuantity().compareTo(matchQuantity) < 0) {
				throw new IllegalArgumentException(
						"Not enough remaining quantity!");
			}
		}

		@Override
		void apply(Orderbook orderbook) {
			NavigableMap<BigDecimal, List<Order>> map = orderbook.getMap(order
					.getSide());
			List<Order> list = map.get(order.getPrice());

			Order firstOrder = list.get(0);
			if (firstOrder.getId() != order.getId()) {
				throw new IllegalArgumentException("Order " + order.getId()
						+ " isn't first order: " + firstOrder);
			}

			BigDecimal newFilledQuantity = firstOrder.getFilledQuantity().add(
					matchQuantity);
			if (newFilledQuantity.compareTo(firstOrder.getQuantity()) >= 0) {
				list.remove(0);
				if (list.isEmpty()) {
					map.remove(firstOrder.getPrice());
				}
				return;
			}

			list.set(0, new Order(firstOrder.getId(), firstOrder.getSide(),
					firstOrder.getMarket(), firstOrder.getQuantity(),
					newFilledQuantity, firstOrder.getPrice()));
		}

		public static final class FillOperationExternalizer implements
				Externalizer<FillOperation> {
			private static final long serialVersionUID = 8602552311427149278L;

			@Override
			public void writeObject(ObjectOutput output, FillOperation object)
					throws IOException {
				output.writeInt(VERSION);
				output.writeObject(object.order);
				output.writeObject(object.matchQuantity);
			}

			@Override
			public FillOperation readObject(ObjectInput input)
					throws IOException, ClassNotFoundException {
				int version = input.readInt();
				if (version != VERSION) {
					throw new IllegalArgumentException(
							"Don't know how to read FillOperation version "
									+ version);
				}
				return new FillOperation((Order) input.readObject(),
						(BigDecimal) input.readObject());
			}

		}
	}

	@SerializeWith(Orderbook.OrderbookDelta.OrderbookDeltaExternalizer.class)
	public static final class OrderbookDelta implements Delta {
		private final static int VERSION = 1;

		private final List<OrderbookOperation> operations;

		private OrderbookDelta(List<OrderbookOperation> operations) {
			this.operations = new ArrayList<>(operations);
		}

		@Override
		public DeltaAware merge(DeltaAware deltaAware) {
			Orderbook orderbook;
			if (deltaAware instanceof Orderbook) {
				orderbook = (Orderbook) deltaAware;
			} else {
				orderbook = new Orderbook();
			}

			for (OrderbookOperation operation : operations) {
				operation.apply(orderbook);
			}

			return orderbook;
		}

		private void addOperation(Orderbook orderbook,
				OrderbookOperation operation) {
			operation.apply(orderbook);
			operations.add(operation);
		}

		private void clear() {
			operations.clear();
		}

		private OrderbookDelta getCopy() {
			return new OrderbookDelta(this.operations);
		}

		public static final class OrderbookDeltaExternalizer implements
				Externalizer<OrderbookDelta> {
			private static final long serialVersionUID = -495558458129731541L;

			@Override
			public void writeObject(ObjectOutput output, OrderbookDelta object)
					throws IOException {
				output.writeInt(VERSION);
				output.writeInt(object.operations.size());
				for (OrderbookOperation operation : object.operations) {
					output.writeObject(operation);
				}
			}

			@Override
			public OrderbookDelta readObject(ObjectInput input)
					throws IOException, ClassNotFoundException {
				int version = input.readInt();
				if (version != VERSION) {
					throw new IllegalArgumentException(
							"Don't know how to read OrderbookDelta version "
									+ version);
				}

				int size = input.readInt();
				List<OrderbookOperation> operations = new ArrayList<>(size);
				for (int i = 0; i < size; ++i) {
					operations.add((OrderbookOperation) input.readObject());
				}
				return new OrderbookDelta(operations);
			}
		}
	}

	public static final class OrderbookExternalizer implements
			Externalizer<Orderbook> {
		private static final long serialVersionUID = -2785338400384460549L;

		@Override
		public void writeObject(ObjectOutput output, Orderbook object)
				throws IOException {
			output.writeObject(object.delta);
		}

		@Override
		public Orderbook readObject(ObjectInput input) throws IOException,
				ClassNotFoundException {
			OrderbookDelta delta = (OrderbookDelta) input.readObject();
			return (Orderbook) delta.merge(null);
		}

	}
}
