package com.blogspot.hypefree.infinispantest.source;

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.blogspot.hypefree.infinispantest.Order;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public final class Source {
	private final Client client = Client.create();
	private int ipSuffix = 1;

	private void faultTolerantQuery(String URI,
			MultivaluedMap<String, String> params) {
		while (true) {
			try {
				ClientResponse response = client
						.resource("http://127.0.0." + ipSuffix + ":8082" + URI)
						.accept(MediaType.WILDCARD)
						.type(MediaType.APPLICATION_FORM_URLENCODED)
						.post(ClientResponse.class, params);
				if (response.getStatus() >= 200 && response.getStatus() < 300) {
					return;
				}
			} catch (ClientHandlerException ex) {
			}
			ipSuffix += 1;
			if (ipSuffix == 255) {
				ipSuffix = 1;
			}
		}
	}

	private void run() throws IOException {
		DataSource dataSource = new DataSource();
		int i = 0;
		while (dataSource.hasNext()) {
			Order order = dataSource.next();

			MultivaluedMap<String, String> parameters = new MultivaluedMapImpl();
			parameters.add("id", Long.toString(order.getId()));
			parameters.add("side", order.getSide().name());
			parameters.add("market", order.getMarket().name());
			parameters.add("quantity", order.getQuantity().toString());
			parameters.add("price", order.getPrice().toString());

			faultTolerantQuery("/order", parameters);

			if (++i % 100 == 0) {
				System.out.println(i);
			}
		}
		dataSource.close();
	}

	public static void main(String[] args) throws IOException {
		new Source().run();
	}
}
