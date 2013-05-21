package com.blogspot.hypefree.infinispantest.venue;

import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.blogspot.hypefree.infinispantest.Market;
import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Side;

@Path("/order")
public final class OrderHandler {
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void createOrder(MultivaluedMap<String, String> form) {
		Side sideValue = Side.valueOf(form.getFirst("side"));
		Market marketValue = Market.valueOf(form.getFirst("market"));
		BigDecimal quantityValue = new BigDecimal(form.getFirst("quantity")), priceValue = new BigDecimal(
				form.getFirst("price"));
		Order order = new Order(Long.valueOf(form.getFirst("id")), sideValue, marketValue, quantityValue,
				BigDecimal.ZERO, priceValue);
		
		try {
			Venue.getInstance().addOrder(order).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new WebApplicationException(e, 500);
		}
	}
}
