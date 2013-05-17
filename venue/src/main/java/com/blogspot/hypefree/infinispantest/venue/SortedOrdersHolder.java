package com.blogspot.hypefree.infinispantest.venue;

import java.util.*;

import org.apache.commons.lang3.math.Fraction;

final class SortedOrdersHolder {
	private final NavigableMap<Fraction, List<Long>> orderIdsPerPrice = new TreeMap<>();

	private List<Long> getList(Fraction price) {
		List<Long> result = orderIdsPerPrice.get(price);
		if (result != null) {
			return result;
		}
		result = new ArrayList<>();
		orderIdsPerPrice.put(price, result);
		return result;
	}

	void addOrderId(Fraction price, Long orderId) {
		List<Long> list = getList(price);
		assert !list.contains(orderId);
		list.add(orderId);
	}

	void removeOrderId(Fraction price, Long orderId) {
		List<Long> list = getList(price);
		list.remove(orderId);
		if (list.isEmpty()) {
			orderIdsPerPrice.remove(price);
		}
	}
	
	boolean isEmpty() {
		return orderIdsPerPrice.isEmpty();
	}
	
	Fraction getLowestPrice() {
		return orderIdsPerPrice.firstKey();
	}
	
	Fraction getHighestPrice() {
		return orderIdsPerPrice.lastKey();
	}

	List<Long> getOrderIdsAtPrice(Fraction price) {
		return Collections.unmodifiableList(new ArrayList<>(orderIdsPerPrice.get(price)));
	}

	Fraction getPriceLowerOrEqual(Fraction price) {
		return orderIdsPerPrice.floorKey(price);
	}

	void clear() {
		orderIdsPerPrice.clear();
	}
}
