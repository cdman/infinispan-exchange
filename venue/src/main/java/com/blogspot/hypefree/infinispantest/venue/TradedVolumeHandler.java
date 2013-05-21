package com.blogspot.hypefree.infinispantest.venue;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/volume")
public class TradedVolumeHandler {
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getTradedVolume() {
		return Double.toString(Venue.getInstance().getTradedVolume());
	}
}
