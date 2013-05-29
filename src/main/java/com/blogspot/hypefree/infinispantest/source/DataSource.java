package com.blogspot.hypefree.infinispantest.source;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import com.blogspot.hypefree.infinispantest.Market;
import com.blogspot.hypefree.infinispantest.Order;
import com.blogspot.hypefree.infinispantest.Side;

public final class DataSource implements Iterator<Order> {
	private static final Pattern SPLIT_PATTERN = Pattern.compile("[ @]");
	private static int iterationCount;

	private final BufferedReader in;
	private final long idHi;
	private long lineCount;
	private String line;

	public DataSource() throws IOException {
		idHi = (long) System.currentTimeMillis() << 31L + (long) (++iterationCount);
		in = new BufferedReader(new InputStreamReader(new GZIPInputStream(
				getClass().getResourceAsStream("/ops.txt.gz"))));
		line = in.readLine();
	}

	@Override
	public boolean hasNext() {
		return line != null;
	}

	@Override
	public Order next() {
		String[] parts = SPLIT_PATTERN.split(line);
		assert parts.length == 3;

		long id = idHi + (++lineCount);
		Side side = null;
		if (parts[0].equals("buy")) {
			side = Side.BUY;
		} else if (parts[0].equals("sell")) {
			side = Side.SELL;
		} else {
			assert false;
		}

		BigDecimal quantity = new BigDecimal(parts[1]);
		BigDecimal price = new BigDecimal(parts[2]);

		try {
			line = in.readLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return new Order(id, side, Market.BTCUSD, quantity, BigDecimal.ZERO,
				price);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public void close() throws IOException {
		in.close();
	}

}
