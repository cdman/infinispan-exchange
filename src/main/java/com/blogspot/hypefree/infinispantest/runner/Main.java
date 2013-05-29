package com.blogspot.hypefree.infinispantest.runner;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

public final class Main {
	private static final Log LOG = LogFactory.getLog(Main.class);
	private static final CountDownLatch INITIAL_VENUES_STARTED = new CountDownLatch(
			1);

	private static ProcessBuilder getVenueProcessBuilder(int index)
			throws IOException {
		List<String> arguments = new ArrayList<>();
		// the java executable
		arguments
				.add(String.format("%s%sbin%sjava",
						System.getProperty("java.home"), File.separator,
						File.separator));
		// pre-execuable arguments (like -D, -agent, etc)
		arguments.addAll(ManagementFactory.getRuntimeMXBean()
				.getInputArguments());

		String classPath = System.getProperty("java.class.path");
		arguments.add("-classpath");
		arguments.add(classPath);

		Iterator<String> it = arguments.iterator();
		while (it.hasNext()) {
			String argument = it.next();
			if (argument.startsWith("-agentlib:")) {
				it.remove();
			}
		}

		arguments.add(com.blogspot.hypefree.infinispantest.venue.Main.class
				.getCanonicalName());

		File output = File.createTempFile("venue-log-" + index + "-", ".log");
		File error = File.createTempFile("venue-error-" + index + "-", ".log");
		return new ProcessBuilder(arguments).redirectOutput(output)
				.redirectError(error);
	}

	private static Runnable getDaemonRunnable() {
		return new Runnable() {
			private final static int NO_VENUES = 3;

			private final Process[] processes = new Process[NO_VENUES];

			private void execute() throws Exception {
				Field fId = Class.forName("java.lang.UNIXProcess")
						.getDeclaredField("pid");
				fId.setAccessible(true);

				for (int i = 0; i < processes.length; ++i) {
					LOG.info("Starting venue " + i);
					processes[i] = getVenueProcessBuilder(i).start();
					TimeUnit.SECONDS.sleep(5);
				}
				INITIAL_VENUES_STARTED.countDown();

				Random r = new Random();
				while (true) {
					TimeUnit.MINUTES.sleep(5);

					int i = r.nextInt(processes.length);

					LOG.info("Stopping venue " + i);
					processes[i].destroy();
					TimeUnit.SECONDS.sleep(1);

					int pid = fId.getInt(processes[i]);
					LOG.info("Killing pid " + pid);
					Runtime.getRuntime()
							.exec(new String[] { "/bin/kill", "-9",
									Integer.toString(pid) }).waitFor();

					LOG.info("Waiting for termination " + i);
					processes[i].waitFor();

					TimeUnit.SECONDS.sleep(10);
					LOG.info("Starting venue " + i);
					processes[i] = getVenueProcessBuilder(i).start();
				}
			}

			@Override
			public void run() {
				try {
					execute();
				} catch (InterruptedException e) {
					return;
				} catch (Exception e) {
					LOG.error("Error", e);
					return;
				} finally {
					for (Process process : processes) {
						if (process == null) {
							continue;
						}
						try {
							process.destroy();
						} catch (Exception e) {
							LOG.error("Error", e);
						}
					}
				}
			}
		};
	}

	public static void main(String[] args) throws Exception {
		Thread daemonizer = new Thread(getDaemonRunnable(), "Venue manager");
		daemonizer.start();
		INITIAL_VENUES_STARTED.await();

		try {
			RestSource restSource = new RestSource();
			restSource.run();
			LOG.info("Total quantity: " + restSource.getTotalQuantity());
		} finally {
			daemonizer.interrupt();
		}
	}
}
