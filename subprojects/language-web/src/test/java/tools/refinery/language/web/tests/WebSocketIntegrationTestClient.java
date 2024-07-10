/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.tests;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public abstract class WebSocketIntegrationTestClient {
	private static final long TIMEOUT_MILLIS = Duration.ofSeconds(10).toMillis();

	private boolean finished = false;

	private final Object lock = new Object();

	private Throwable error;

	private int closeStatusCode;

	private final List<String> responses = new ArrayList<>();

	public int getCloseStatusCode() {
		return closeStatusCode;
	}

	public List<String> getResponses() {
		return responses;
	}

	@OnWebSocketOpen
	public void onOpen(Session session) {
		arrangeAndCatchErrors(session);
	}

	private void arrangeAndCatchErrors(Session session) {
		try {
			arrange(session, responses.size());
		} catch (Exception e) {
			finishedWithError(e);
		}
	}

	protected abstract void arrange(Session session, int responsesReceived);

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		closeStatusCode = statusCode;
		testFinished();
	}

	@OnWebSocketError
	public void onError(Throwable error) {
		finishedWithError(error);
	}

	@OnWebSocketMessage
	public void onMessage(Session session, String message) {
		responses.add(message);
		arrangeAndCatchErrors(session);
	}

	private void finishedWithError(Throwable t) {
		error = t;
		testFinished();
	}

	private void testFinished() {
		synchronized (lock) {
			finished = true;
			lock.notify();
		}
	}

	public void waitForTestResult() {
		synchronized (lock) {
			if (!finished) {
				try {
					lock.wait(TIMEOUT_MILLIS);
				} catch (InterruptedException e) {
					fail("Unexpected InterruptedException", e);
				}
			}
		}
		if (!finished) {
			fail("Test still not finished after timeout");
		}
		if (error != null) {
			fail("Unexpected exception in websocket thread", error);
		}
	}
}
