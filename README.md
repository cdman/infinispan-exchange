Infinispan Exchange Simulator
===

This is a sample project using [Infinispan](http://www.jboss.org/infinispan/) to implement a fault-tolerant electronic exchange. The accompanying article will appear in [TodaySoftMag](http://todaysoftmag.com). It uses transaction data captured from [MtGox](https://mtgox.com/) for benchmarking purposes.

Ways to run it:

- It is a standard Java project with Maven.
- You can run ````SimpleTimingTest```` to see the raw, local performance
- You can run multiple (at least 3) instances of ````venue.Main```` with the system parameter ````-DlocalLoad```` to see the performance of loading it locally, from the same node hosting the cache
- You can run ````runner.Main```` to execute a benchmark which will start 3 nodes, insert the orders into them while in parallel restarting a random node every couple of minutes to check the reliability

Copyright Attila-Mihály Balázs

Available under the Apache Public License v2

