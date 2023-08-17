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

public class RestartableCachedThreadPool implements ExecutorService {
	private static final Logger LOG = LoggerFactory.getLogger(RestartableCachedThreadPool.class);

	private ExecutorService delegate;

	private final Provider<ExecutorService> executorServiceProvider;

	public RestartableCachedThreadPool(Provider<ExecutorService> executorServiceProvider) {
		this.executorServiceProvider = executorServiceProvider;
		delegate = executorServiceProvider.get();
	}

	public void waitForAllTasksToFinish() {
		delegate.shutdown();
		waitForTermination();
		delegate = executorServiceProvider.get();
	}

	public void waitForTermination() {
		boolean result = false;
		try {
			result = delegate.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOG.warn("Interrupted while waiting for delegate executor to stop", e);
		}
		if (!result) {
			throw new IllegalStateException("Failed to shut down Xtext thread pool");
		}
	}

	@Override
	public boolean awaitTermination(long arg0, @NotNull TimeUnit arg1) throws InterruptedException {
		return delegate.awaitTermination(arg0, arg1);
	}

	@Override
	public void execute(@NotNull Runnable arg0) {
		delegate.execute(arg0);
	}

	@Override
	public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> arg0, long arg1,
										 @NotNull TimeUnit arg2)
			throws InterruptedException {
		return delegate.invokeAll(arg0, arg1, arg2);
	}

	@Override
	public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> arg0) throws InterruptedException {
		return delegate.invokeAll(arg0);
	}

	@Override
	public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> arg0, long arg1, @NotNull TimeUnit arg2)
			throws InterruptedException, ExecutionException, TimeoutException {
		return delegate.invokeAny(arg0, arg1, arg2);
	}

	@Override
	public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> arg0) throws InterruptedException,
			ExecutionException {
		return delegate.invokeAny(arg0);
	}

	@Override
	public boolean isShutdown() {
		return delegate.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return delegate.isTerminated();
	}

	@Override
	public void shutdown() {
		delegate.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return delegate.shutdownNow();
	}

	@Override
	public <T> Future<T> submit(@NotNull Callable<T> arg0) {
		return delegate.submit(arg0);
	}

	@Override
	public <T> Future<T> submit(@NotNull Runnable arg0, T arg1) {
		return delegate.submit(arg0, arg1);
	}

	@Override
	public Future<?> submit(@NotNull Runnable arg0) {
		return delegate.submit(arg0);
	}
}
