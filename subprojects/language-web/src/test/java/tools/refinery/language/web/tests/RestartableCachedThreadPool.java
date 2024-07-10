/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.tests;

import com.google.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class RestartableCachedThreadPool implements ExecutorService {
	private static final Logger LOG = LoggerFactory.getLogger(RestartableCachedThreadPool.class);

	private final AtomicReference<ExecutorService> delegate = new AtomicReference<>();

	private final Provider<ExecutorService> executorServiceProvider;

	public RestartableCachedThreadPool(Provider<ExecutorService> executorServiceProvider) {
		this.executorServiceProvider = executorServiceProvider;
		delegate.set(executorServiceProvider.get());
	}

	public void waitForAllTasksToFinish() {
		var oldDelegate = delegate.getAndSet(executorServiceProvider.get());
		oldDelegate.shutdown();
		waitForTermination(oldDelegate);
	}

	public void waitForTermination() {
		waitForTermination(delegate.get());
	}

	private static void waitForTermination(ExecutorService executorService) {
		boolean result = false;
		try {
			result = executorService.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOG.warn("Interrupted while waiting for delegate executor to stop", e);
		}
		if (!result) {
			throw new IllegalStateException("Failed to shut down Xtext thread pool");
		}
	}

	@Override
	public boolean awaitTermination(long arg0, @NotNull TimeUnit arg1) throws InterruptedException {
		return delegate.get().awaitTermination(arg0, arg1);
	}

	@Override
	public void execute(@NotNull Runnable arg0) {
		delegate.get().execute(arg0);
	}

	@Override
	public <T> @NotNull List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> arg0, long arg1,
												  @NotNull TimeUnit arg2)
			throws InterruptedException {
		return delegate.get().invokeAll(arg0, arg1, arg2);
	}

	@Override
	public <T> @NotNull List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> arg0)
			throws InterruptedException {
		return delegate.get().invokeAll(arg0);
	}

	@Override
	public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> arg0, long arg1, @NotNull TimeUnit arg2)
			throws InterruptedException, ExecutionException, TimeoutException {
		return delegate.get().invokeAny(arg0, arg1, arg2);
	}

	@Override
	public <T> @NotNull T invokeAny(@NotNull Collection<? extends Callable<T>> arg0) throws InterruptedException,
			ExecutionException {
		return delegate.get().invokeAny(arg0);
	}

	@Override
	public boolean isShutdown() {
		return delegate.get().isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return delegate.get().isTerminated();
	}

	@Override
	public void shutdown() {
		delegate.get().shutdown();
	}

	@Override
	public @NotNull List<Runnable> shutdownNow() {
		return delegate.get().shutdownNow();
	}

	@Override
	public <T> @NotNull Future<T> submit(@NotNull Callable<T> arg0) {
		return delegate.get().submit(arg0);
	}

	@Override
	public <T> @NotNull Future<T> submit(@NotNull Runnable arg0, T arg1) {
		return delegate.get().submit(arg0, arg1);
	}

	@Override
	public @NotNull Future<?> submit(@NotNull Runnable arg0) {
		return delegate.get().submit(arg0);
	}
}
