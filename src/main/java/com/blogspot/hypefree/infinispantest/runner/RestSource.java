package com.blogspot.hypefree.infinispantest.runner;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.source.DataSource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

final class RestSource {
	private static final Log LOG = LogFactory.getLog(RestSource.class);

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

			try {
				TimeUnit.MILLISECONDS.sleep(10);
			} catch (InterruptedException e) {
			}

			ipSuffix += 1;
			if (ipSuffix == 20) {
				ipSuffix = 1;
			}
		}
	}

	double getTotalQuantity() {
		try {
			ClientResponse response = client
					.resource("http://127.0.0." + ipSuffix + ":8082/volume")
					.accept(MediaType.WILDCARD).get(ClientResponse.class);
			return Double.parseDouble(response.getEntity(String.class));
		} catch (Exception ex) {
			return Double.NaN;
		}
	}

	void run() throws IOException {
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
				LOG.info("Inserted " + i + " orders");
				LOG.info("Total quantity: " + getTotalQuantity());
			}
		}
		dataSource.close();
	}
}
