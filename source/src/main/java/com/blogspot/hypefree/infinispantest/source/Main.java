package com.blogspot.hypefree.infinispantest.source;

import java.io.IOException;

public final class Main {
	public static void main(String[] args) throws IOException {
		Source source = new Source(10);
		source.run();
	}
}
