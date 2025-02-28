/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api.sink;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.language.web.api.ScheduledWorker;
import tools.refinery.language.web.api.dto.RefineryResponse;

import java.io.EOFException;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.*;

public class SseResponseSink implements ResponseSink {
	private static final Logger LOG = LoggerFactory.getLogger(SseResponseSink.class);

	/**
	 * Interval between heartbeats to check whether the client is still listening.
	 * <p>
	 * Since this is our only way to know whether the client has cancelled the streaming action, the interval has to
	 * be short enough so that we don't waste much compute on already cancelled tasks. However, we must not make it
	 * too frequent due to the network overhead of sending a heartbeat.
	 * </p>
	 */
	private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(1);

	private final SseEventSink eventSink;
	private final Sse sse;

	public SseResponseSink(SseEventSink eventSink, Sse sse) {
		this.eventSink = eventSink;
		this.sse = sse;
	}

	@Override
	public void setResponse(int ignoredStatusCode, RefineryResponse response) {
		LOG.debug("Worker returned result");
		if (isCancelled()) {
			return;
		}
		try {
			eventSink.send(sse.newEventBuilder()
							.mediaType(MediaType.APPLICATION_JSON_TYPE)
							.data(response)
							.build())
					.toCompletableFuture()
					.join();
		} catch (CompletionException e) {
			if (e.getCause() instanceof EofException) {
				// Ignore exception, since the client has already disconnected.
				return;
			}
			throw e;
		}
	}

	@Override
	public void updateStatus(Object status) {
		LOG.debug("Worker status update: {}", status);
		if (isCancelled()) {
			return;
		}
		try {
			eventSink.send(sse.newEventBuilder()
							.mediaType(MediaType.APPLICATION_JSON_TYPE)
							.data(new RefineryResponse.Status(status))
							.build())
					.toCompletableFuture()
					.join();
		} catch (CompletionException e) {
			if (e.getCause() instanceof EofException) {
				// Ignore exception, since the client has already disconnected.
				return;
			}
			throw e;
		}
	}

	@Override
	public boolean isCancelled() {
		return eventSink.isClosed();
	}

	public void loop(ScheduledWorker<?> worker) throws InterruptedException {
		while (worker.isRunning()) {
			boolean finished;
			try {
				worker.poll(HEARTBEAT_INTERVAL);
				finished = true;
			} catch (ExecutionException e) {
				// This should never happen, because the worker will handle its own exceptions.
				LOG.error("Uncaught exception in worker", e);
				finished = true;
			} catch (CancellationException e) {
				// Operation was already cancelled.
				finished = true;
			} catch (TimeoutException e) {
				finished = false;
			}
			if (finished) {
				break;
			}
			LOG.trace("Sending SSE heartbeat");
			sendHeartbeat(worker);
		}
		closeSink();
	}

	private void sendHeartbeat(ScheduledWorker<?> worker) {
		try {
			if (!isCancelled()) {
				// Send and empty comment to check whether the client is still connected. See
				// https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#event_stream_format
				eventSink.send(sse.newEventBuilder()
								.comment("")
								.build())
						.toCompletableFuture()
						.join();
			}
		} catch (CompletionException e) {
			if (e.getCause() instanceof EOFException) {
				handleDisconnect(worker);
			} else {
				throw e;
			}
		}
	}

	private void handleDisconnect(ScheduledWorker<?> worker) {
		LOG.debug("Client has disconnected, cancelling worker");
		closeSink();
		worker.cancel();
	}

	private void closeSink() {
		if (isCancelled()) {
			return;
		}
		try {
			eventSink.close();
		} catch (IOException e) {
			LOG.error("Failed to close SSE sink", e);
		}
	}
}
