/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server;

import com.google.inject.Singleton;
import org.eclipse.xtext.ide.ExecutorServiceProvider;
import org.eclipse.xtext.web.server.model.XtextWebDocumentAccess;
import org.jetbrains.annotations.NotNull;
import tools.refinery.language.web.semantics.SemanticsService;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class ThreadPoolExecutorServiceProvider extends ExecutorServiceProvider {
	private static final String DOCUMENT_LOCK_EXECUTOR;
	private static final AtomicInteger POOL_ID = new AtomicInteger(1);

	private final Map<String, ScheduledExecutorService> scheduledInstanceCache =
			Collections.synchronizedMap(new HashMap<>());
	private final int executorThreadCount;
	private final int lockExecutorThreadCount;
	private final int semanticsExecutorThreadCount;

	static {
		var lookup = MethodHandles.lookup();
		MethodHandle getter;
		try {
			var privateLookup = MethodHandles.privateLookupIn(XtextWebDocumentAccess.class, lookup);
			getter = privateLookup.findStaticGetter(XtextWebDocumentAccess.class, "DOCUMENT_LOCK_EXECUTOR",
					String.class);
		} catch (IllegalAccessException | NoSuchFieldException e) {
			throw new IllegalStateException("Failed to find getter", e);
		}
		try {
			DOCUMENT_LOCK_EXECUTOR = (String) getter.invokeExact();
		} catch (Error e) {
			// Rethrow JVM errors.
			throw e;
		} catch (Throwable e) {
			throw new IllegalStateException("Failed to get DOCUMENT_LOCK_EXECUTOR", e);
		}
	}

	public ThreadPoolExecutorServiceProvider() {
		executorThreadCount = getCount("REFINERY_XTEXT_THREAD_COUNT").orElse(0);
		lockExecutorThreadCount = getCount("REFINERY_XTEXT_LOCKING_THREAD_COUNT").orElse(executorThreadCount);
		semanticsExecutorThreadCount = getCount("REFINERY_XTEXT_SEMANTICS_THREAD_COUNT").orElse(executorThreadCount);
	}

	private static Optional<Integer> getCount(String name) {
		return Optional.ofNullable(System.getenv(name)).map(Integer::parseUnsignedInt);
	}

	public ScheduledExecutorService getScheduled(String key) {
		return scheduledInstanceCache.computeIfAbsent(key, this::createScheduledInstance);
	}

	@Override
	protected ExecutorService createInstance(String key) {
		String name = "xtext-" + POOL_ID.getAndIncrement();
		if (key != null) {
			name = name + "-" + key;
		}
		var threadFactory = new Factory(name, 5);
		int size = getSize(key);
		if (size == 0) {
			return Executors.newCachedThreadPool(threadFactory);
		}
		return Executors.newFixedThreadPool(size, threadFactory);
	}

	protected ScheduledExecutorService createScheduledInstance(String key) {
		String name = "xtext-scheduled-" + POOL_ID.getAndIncrement();
		if (key != null) {
			name = name + "-" + key;
		}
		var threadFactory = new Factory(name, 5);
		return Executors.newScheduledThreadPool(1, threadFactory);
	}

	private int getSize(String key) {
		if (SemanticsService.SEMANTICS_EXECUTOR.equals(key)) {
			return semanticsExecutorThreadCount;
		} else if (DOCUMENT_LOCK_EXECUTOR.equals(key)) {
			return lockExecutorThreadCount;
		} else {
			return executorThreadCount;
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		synchronized (scheduledInstanceCache) {
			for (var instance : scheduledInstanceCache.values()) {
				instance.shutdown();
			}
			scheduledInstanceCache.clear();
		}
	}

	private static class Factory implements ThreadFactory {
		// We have to explicitly store the {@link ThreadGroup} to create a {@link ThreadFactory}.
		@SuppressWarnings("squid:S3014")
		private final ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
		private final AtomicInteger threadId = new AtomicInteger(1);
		private final String namePrefix;
		private final int priority;

		public Factory(String name, int priority) {
			namePrefix = name + "-thread-";
			this.priority = priority;
		}

		@Override
		public Thread newThread(@NotNull Runnable runnable) {
			var thread = new Thread(threadGroup, runnable, namePrefix + threadId.getAndIncrement());
			if (thread.isDaemon()) {
				thread.setDaemon(false);
			}
			if (thread.getPriority() != priority) {
				thread.setPriority(priority);
			}
			return thread;
		}
	}
}
