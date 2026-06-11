/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.context;

import com.microsoft.z3.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.store.util.CancellationToken;

import java.util.concurrent.*;

public class InterruptibleWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(InterruptibleWrapper.class);

	private final CancellationToken cancellationToken;
	private final Context context;
	private final ExecutorService executor;

	private Thread workerThread;
	private Future<?> currentFuture;
	private boolean terminated;

	public InterruptibleWrapper(CancellationToken cancellationToken, Context context) {
		this.cancellationToken = cancellationToken;
		this.context = context;
		this.executor = Executors.newSingleThreadExecutor(runnable -> {
			var thread = new Thread(() -> {
				try {
					runnable.run();
				} finally {
					context.close();
				}
			}, "SMT-worker");
			thread.setDaemon(true);
			workerThread = thread;
			return thread;
		});
	}

	public <T> T call(Callable<T> callable) {
		if (terminated) {
			throw new CancellationException("SMT solver was already interrupted");
		}
		if (currentFuture != null) {
			throw new IllegalStateException("A task is already pending");
		}

		Future<T> future = executor.submit(callable);
		currentFuture = future;
		try {
			while (true) {
				try {
					return future.get(100, TimeUnit.MILLISECONDS);
				} catch (TimeoutException e) {
					try {
						cancellationToken.checkCancelled();
					} catch (RuntimeException cancelled) {
						shutdown();
						throw cancelled;
					}
				} catch (InterruptedException e) {
					shutdown();
					// Only restore the interrupt flag if we have managed to shut down the `ExecutorService`
					// or logged that Z3 got stuck.
					Thread.currentThread().interrupt();
					var cancellationException = new CancellationException("Interrupted while waiting for SMT result");
					cancellationException.initCause(e);
					throw cancellationException;
				} catch (ExecutionException e) {
					throw rethrow(e.getCause());
				}
			}
		} finally {
			currentFuture = null;
		}
	}

	public void shutdown() {
		if (terminated) {
			return;
		}
		terminated = true;

		context.interrupt();

		if (currentFuture != null) {
			currentFuture.cancel(true);
		}

		executor.shutdown();

		boolean success;
		try {
			success = executor.awaitTermination(100, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			success = false;
			LOGGER.warn("Interrupted while waiting for executor termination", e);
		}
		if (!success) {
			executor.shutdownNow();
			// We intentionally leak the thread with the stuck Z3 instance for rapid response,
			// but lower its priority to avoid excessive CPU usage
			if (workerThread != null) {
				workerThread.setPriority(Thread.MIN_PRIORITY);
			}
			LOGGER.warn("Z3 is still running");
		}
	}

	private RuntimeException rethrow(Throwable cause) {
		if (cause instanceof RuntimeException runtimeException) {
			return runtimeException;
		}
		if (cause instanceof Error error) {
			throw error;
		}
		return new RuntimeException(cause);
	}
}
