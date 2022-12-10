package tools.refinery.language.web;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VirtualThreadUtils {
	private VirtualThreadUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static ExecutorService newNamedVirtualThreadsExecutor(String name) {
		// Based on
		// https://github.com/eclipse/jetty.project/blob/83154b4ffe4767ef44981598d6c26e6a5d32e57c/jetty-server/src/main/config/etc/jetty-threadpool-virtual-preview.xml
		return Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
				.allowSetThreadLocals(true)
				.inheritInheritableThreadLocals(false)
				.name(name + "-virtual-", 0)
				.factory());
	}

	public static ThreadPool newThreadPoolWithVirtualThreadsExecutor(String name) {
		// Based on
		// https://github.com/eclipse/jetty.project/blob/83154b4ffe4767ef44981598d6c26e6a5d32e57c/jetty-server/src/main/config/etc/jetty-threadpool-virtual-preview.xml
		int timeout = (int) Duration.ofMinutes(1).toMillis();
		var threadPool = new QueuedThreadPool(200, 10, timeout, -1, null, null);
		threadPool.setName(name);
		threadPool.setDetailedDump(false);
		threadPool.setVirtualThreadsExecutor(newNamedVirtualThreadsExecutor(name));
		return threadPool;
	}

	public static Server newServerWithVirtualThreadsThreadPool(String name, InetSocketAddress listenAddress) {
		var server = new Server(newThreadPoolWithVirtualThreadsExecutor(name));
		var connector = new ServerConnector(server);
		try {
			connector.setHost(listenAddress.getHostName());
			connector.setPort(listenAddress.getPort());
			server.addConnector(connector);
		} catch (Exception e) {
			connector.close();
			throw e;
		}
		return server;
	}
}
