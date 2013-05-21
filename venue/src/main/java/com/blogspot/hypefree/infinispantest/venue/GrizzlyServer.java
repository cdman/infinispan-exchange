package com.blogspot.hypefree.infinispantest.venue;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

final class GrizzlyServer {
	private static final Log LOG = LogFactory.getLog(GrizzlyServer.class);

	void start() {
		ResourceConfig rc = new ClassNamesResourceConfig(OrderHandler.class,
				TradedVolumeHandler.class);

		for (int i = 1; i < 255; ++i) {
			URI serverURI = UriBuilder.fromUri("http://127.0.0." + i + "/")
					.port(8082).build();
			try {
				GrizzlyServerFactory.createHttpServer(serverURI, rc);
				LOG.info("Bound to " + serverURI);
				break;
			} catch (Exception e) {
				LOG.info("Error binding to " + serverURI + ": "
						+ e.getMessage());
				continue;
			}
		}
	}
}
