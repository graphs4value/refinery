/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api;

import com.google.inject.Inject;
import org.eclipse.xtext.service.OperationCanceledError;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.language.web.api.dto.RefineryResponse;
import tools.refinery.language.web.api.provider.ServerExceptionMapper;
import tools.refinery.language.web.api.sink.ResponseSink;
import tools.refinery.language.web.xtext.server.ThreadPoolExecutorServiceProvider;
import tools.refinery.store.util.CancellationToken;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ScheduledWorker<T> {
	private static final Logger LOG = LoggerFactory.getLogger(ScheduledWorker.class);

	@Inject
	private OperationCanceledManager operationCanceledManager;

	@Inject
	private ServerExceptionMapper serverExceptionMapper;

	private T request;
	private ResponseSink responseSink;
	private CancellationToken cancellationToken;
	private ExecutorService executorService;
	private final ReentrantLock lock = new ReentrantLock();
	private Future<?> future;
	private ScheduledExecutorService scheduledExecutorService;
	private ScheduledFuture<?> timeoutFuture;
	private volatile boolean cancelled;
	private volatile boolean timedOut;
	private volatile boolean responseSet;

	@Inject
	public void setExecutorServiceProvider(ThreadPoolExecutorServiceProvider provider) {
		executorService = provider.get(getExecutorServiceKey());
		scheduledExecutorService = provider.getScheduled(
				ThreadPoolExecutorServiceProvider.MODEL_GENERATION_TIMEOUT_EXECUTOR);
	}

	protected abstract String getExecutorServiceKey();

	protected abstract Duration getTimeout();

	protected T getRequest() {
		return request;
	}

	protected CancellationToken getCancellationToken() {
		return cancellationToken;
	}

	public void schedule(T request, ResponseSink responseSink) {
		initialize(request, responseSink);
		schedule();
	}

	protected void initialize(T request, ResponseSink responseSink) {
		this.request = request;
		this.responseSink = responseSink;
		cancellationToken = () -> {
			if (cancelled || timedOut || responseSink.isCancelled() || Thread.interrupted()) {
				operationCanceledManager.throwOperationCanceledException();
			}
		};
	}

	private void schedule() {
		lock.lock();
		try {
			if (future != null || timeoutFuture != null || cancelled || timedOut) {
				throw new IllegalStateException("Worker already scheduled or cancelled");
			}
			var timeout = getTimeout();
			timeoutFuture = scheduledExecutorService.schedule(() -> {
				try {
					LOG.debug("Model generation timed out");
					lock.lock();
					try {
						timedOut = true;
						// Make sure to set the timeout response before cancelling the worker,
						// so it doesn't send an operation cancelled response instead.
						setResponse(RefineryResponse.Timeout.of());
						if (future != null) {
							future.cancel(true);
							future = null;
						}
						timeoutFuture = null;
					} finally {
						lock.unlock();
					}
				} catch (RuntimeException e) {
					LOG.error("Error sending timeout response", e);
				}
			}, timeout.toMillis(), TimeUnit.MILLISECONDS);
			future = executorService.submit(() -> {
				try {
					runWithExceptionMapping();
				} catch (RuntimeException e) {
					// Catch all exceptions here, because we might not get joined to the webserver thread in an
					// asynchronous API call.
					LOG.error("Unhandled exception during model generation", e);
				}
			});
		} finally {
			lock.unlock();
		}
	}

	public void cancel() {
		lock.lock();
		try {
			cancelled = true;
			if (future != null) {
				future.cancel(true);
				future = null;
			}
			if (timeoutFuture != null) {
				timeoutFuture.cancel(true);
				timeoutFuture = null;
			}
			setResponse(new RefineryResponse.Cancelled("Cancelled by server"));
		} finally {
			lock.unlock();
		}
	}

	public boolean isRunning() {
		return !cancelled && !timedOut && future != null && !future.isCancelled();
	}

	public void poll() throws ExecutionException, InterruptedException {
		future.get();
	}

	public void poll(Duration duration) throws ExecutionException, InterruptedException, TimeoutException {
		if (future == null) {
			// Nothing to poll if already cancelled or not yet scheduled.
			return;
		}
		future.get(duration.toMillis(), TimeUnit.MILLISECONDS);
	}

	private void runWithExceptionMapping() {
		try {
			run();
		} catch (OperationCanceledError | Exception e) {
			try (var response = serverExceptionMapper.toResponse(e)) {
				setResponse(response.getStatus(), (RefineryResponse) response.getEntity());
			}
		} finally {
			lock.lock();
			try {
				if (timeoutFuture != null) {
					timeoutFuture.cancel(true);
				}
			} finally {
				lock.unlock();
			}
		}
	}

	protected void setResponse(RefineryResponse response) {
		if (responseSet) {
			return;
		}
		lock.lock();
		try {
			if (responseSet) {
				return;
			}
			responseSink.setResponse(response);
			responseSet = true;
		} finally {
			lock.unlock();
		}
	}

	protected void setResponse(int statusCode, RefineryResponse response) {
		if (responseSet) {
			return;
		}
		lock.lock();
		try {
			if (responseSet) {
				return;
			}
			responseSink.setResponse(statusCode, response);
			responseSet = true;
		} finally {
			lock.unlock();
		}
	}

	protected void updateStatus(Object status) {
		checkCancelled();
		LOG.debug("Status: {}", status);
		responseSink.updateStatus(status);
	}

	protected void checkCancelled() {
		cancellationToken.checkCancelled();
	}

	protected abstract void run() throws IOException;
}
