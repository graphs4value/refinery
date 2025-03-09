/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketTimeoutException;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.web.server.ISession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.language.web.gson.GsonUtil;
import tools.refinery.language.web.xtext.server.ResponseHandler;
import tools.refinery.language.web.xtext.server.ResponseHandlerException;
import tools.refinery.language.web.xtext.server.TransactionExecutor;
import tools.refinery.language.web.xtext.server.message.XtextWebRequest;
import tools.refinery.language.web.xtext.server.message.XtextWebResponse;

import java.io.Reader;
import java.net.SocketAddress;
import java.util.concurrent.locks.ReentrantLock;

@WebSocket
public class XtextWebSocket implements ResponseHandler {
	private static final Logger LOG = LoggerFactory.getLogger(XtextWebSocket.class);

	/**
	 * Maximum number of outgoing frames per WebSocket session to limit buffer resource usage.
	 * <p>
	 * If this limit is exceeded, the WebSocket will throw a {@link java.nio.channels.WritePendingException}, which
	 * will make us close it in {@link #onError(Throwable)}. This is intended, since having such a large backlog
	 * likely indicates a lost or timed out connection, which should be cleaned up.
	 * </p>
	 */
	private static final int MAX_OUTGOING_FRAMES = 10;

	private final Gson gson = GsonUtil.getGson();

	private final TransactionExecutor executor;

	private Session webSocketSession;

	private SocketAddress socketAddress;

	private final ReentrantLock lock = new ReentrantLock();

	public XtextWebSocket(TransactionExecutor executor) {
		this.executor = executor;
		executor.setResponseHandler(this);
	}

	public XtextWebSocket(ISession session, IResourceServiceProvider.Registry resourceServiceProviderRegistry) {
		this(new TransactionExecutor(session, resourceServiceProviderRegistry));
	}

	@OnWebSocketOpen
	public void onOpen(Session webSocketSession) {
		lock.lock();
		try {
			webSocketSession.setMaxOutgoingFrames(MAX_OUTGOING_FRAMES);
			if (this.webSocketSession != null) {
				LOG.error("Websocket session onConnect when already connected");
				return;
			}
			socketAddress = webSocketSession.getRemoteSocketAddress();
			LOG.debug("New websocket connection from {}", socketAddress);
			this.webSocketSession = webSocketSession;
		} finally {
			lock.unlock();
		}
	}

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		lock.lock();
		try {
			executor.dispose();
			if (webSocketSession == null) {
				return;
			}
			if (!webSocketSession.isOpen()) {
				webSocketSession.close();
			}
			webSocketSession = null;
		} finally {
			lock.unlock();
		}
		switch (statusCode) {
		case StatusCode.NORMAL, StatusCode.SHUTDOWN ->
				LOG.debug("{} closed connection normally: {}", socketAddress, reason);
		case StatusCode.NO_CLOSE -> LOG.debug("{} terminated connection without closing", socketAddress);
		default -> LOG.warn("{} closed connection with status code {}: {}", socketAddress, statusCode, reason);
		}
	}

	@OnWebSocketError
	public void onError(Throwable error) {
		lock.lock();
		try {
			executor.dispose();
			if (webSocketSession == null || !webSocketSession.isOpen()) {
				return;
			}
			webSocketSession.close();
		} finally {
			lock.unlock();
		}
		switch (error) {
		case WebSocketTimeoutException ignored -> LOG.warn("Websocket connection from {} timed out", socketAddress,
				error);
		case EofException ignored -> LOG.warn("Websocket connection from {} already closed", socketAddress, error);
		default -> LOG.error("Internal websocket error in connection from {}", socketAddress, error);
		}
	}

	@OnWebSocketMessage
	public void onMessage(Reader reader) {
		XtextWebRequest request = null;
		lock.lock();
		try {
			if (webSocketSession == null) {
				LOG.error("Trying to receive message from {} when websocket is disconnected", socketAddress);
				return;
			}
			try {
				request = gson.fromJson(reader, XtextWebRequest.class);
			} catch (JsonIOException e) {
				LOG.error("Cannot read from websocket from {}", socketAddress, e);
				if (webSocketSession.isOpen()) {
					executor.dispose();
					webSocketSession.close(StatusCode.SERVER_ERROR, "Cannot read payload", Callback.NOOP);
				}
			} catch (JsonParseException e) {
				LOG.warn("Malformed websocket request from {}", socketAddress, e);
				if (webSocketSession.isOpen()) {
					executor.dispose();
					webSocketSession.close(XtextStatusCode.INVALID_JSON, "Invalid JSON payload", Callback.NOOP);
				}
			}
		} finally {
			lock.unlock();
		}
		if (request == null) {
			return;
		}
		try {
			executor.handleRequest(request);
		} catch (ResponseHandlerException e) {
			lock.lock();
			try {
				LOG.warn("Cannot write websocket response to {}", socketAddress, e);
				if (webSocketSession.isOpen()) {
					executor.dispose();
					webSocketSession.close(StatusCode.SERVER_ERROR, "Cannot write response", Callback.NOOP);
				}
			} finally {
				lock.unlock();
			}
		}
	}

	@Override
	public void onResponse(XtextWebResponse response) throws ResponseHandlerException {
		lock.lock();
		try {
			if (webSocketSession == null || !webSocketSession.isOpen()) {
				throw new ResponseHandlerException("Trying to send message to %s when websocket is disconnected"
						.formatted(socketAddress));
			}
			var responseString = gson.toJson(response);
			webSocketSession.sendText(responseString, Callback.from(() -> {
			}, this::writeFailed));
		} finally {
			lock.unlock();
		}
	}

	public void writeFailed(Throwable x) {
		lock.lock();
		try {
			if (webSocketSession == null || !webSocketSession.isOpen()) {
				LOG.warn("Cannot complete async write to disconnected websocket from {}", socketAddress, x);
				return;
			}
			LOG.warn("Cannot complete async write to websocket {}", socketAddress, x);
			webSocketSession.close();
		} finally {
			lock.unlock();
		}
	}
}
