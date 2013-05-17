package com.blogspot.hypefree.infinispantest.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.math.Fraction;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import com.blogspot.hypefree.infinispantest.Constants;
import com.blogspot.hypefree.infinispantest.Market;
import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Side;

public final class Source {
	private static final Pattern SPLIT_PATTERN = Pattern.compile("[ @]");

	private final int repetitionCount;
	private final RemoteCache<Long, Order> orders;

	public Source(int repetitionCount) {
		this.repetitionCount = repetitionCount;
		RemoteCacheManager cacheContainer = new RemoteCacheManager();
		orders = cacheContainer.getCache(Constants.getOrdersCacheName());
	}

	public void run() throws IOException {
		long idHi = (long) System.currentTimeMillis() << 31L;

		for (int i = 0; i < repetitionCount; ++i) {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new GZIPInputStream(getClass().getResourceAsStream(
							"/ops.txt.gz"))));

			String line;
			long lineCount = 0;
			while ((line = in.readLine()) != null) {
				String[] parts = SPLIT_PATTERN.split(line);
				assert parts.length == 3;

				Long id = idHi + (long) i + (++lineCount);
				Side side = null;
				if (parts[0].equals("buy")) {
					side = Side.BUY;
				} else if (parts[0].equals("sell")) {
					side = Side.SELL;
				} else {
					assert false;
				}

				Fraction quantity = Fraction.getFraction(parts[1]);
				Fraction price = Fraction.getFraction(parts[2]);

				orders.put(id, new Order(id, side, Market.BTCUSD, quantity, Fraction.ZERO,
						price));
			}

			in.close();
		}
	}
}