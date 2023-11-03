/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.web.server.ISession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.language.web.semantics.metadata.*;
import tools.refinery.language.web.xtext.server.ResponseHandler;
import tools.refinery.language.web.xtext.server.ResponseHandlerException;
import tools.refinery.language.web.xtext.server.TransactionExecutor;
import tools.refinery.language.web.xtext.server.message.XtextWebRequest;
import tools.refinery.language.web.xtext.server.message.XtextWebResponse;

import java.io.Reader;

@WebSocket
public class XtextWebSocket implements ResponseHandler {
	private static final Logger LOG = LoggerFactory.getLogger(XtextWebSocket.class);

	private final Gson gson = new GsonBuilder()
			.disableJdkUnsafe()
			.registerTypeAdapterFactory(RuntimeTypeAdapterFactory.of(RelationDetail.class, "type")
					.registerSubtype(ClassDetail.class, "class")
					.registerSubtype(ReferenceDetail.class, "reference")
					.registerSubtype(OppositeReferenceDetail.class, "opposite")
					.registerSubtype(PredicateDetail.class, "predicate")
					.registerSubtype(BuiltInDetail.class, "builtin"))
			.create();

	private final TransactionExecutor executor;

	private Session webSocketSession;

	public XtextWebSocket(TransactionExecutor executor) {
		this.executor = executor;
		executor.setResponseHandler(this);
	}

	public XtextWebSocket(ISession session, IResourceServiceProvider.Registry resourceServiceProviderRegistry) {
		this(new TransactionExecutor(session, resourceServiceProviderRegistry));
	}

	@OnWebSocketOpen
	public void onOpen(Session webSocketSession) {
		if (this.webSocketSession != null) {
			LOG.error("Websocket session onConnect when already connected");
			return;
		}
		LOG.debug("New websocket connection from {}", webSocketSession.getRemoteSocketAddress());
		this.webSocketSession = webSocketSession;
	}

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		executor.dispose();
		if (webSocketSession == null) {
			return;
		}
		if (statusCode == StatusCode.NORMAL || statusCode == StatusCode.SHUTDOWN) {
			LOG.debug("{} closed connection normally: {}", webSocketSession.getRemoteSocketAddress(), reason);
		} else {
			LOG.warn("{} closed connection with status code {}: {}", webSocketSession.getRemoteSocketAddress(),
					statusCode, reason);
		}
		webSocketSession = null;
	}

	@OnWebSocketError
	public void onError(Throwable error) {
		executor.dispose();
		if (webSocketSession == null) {
			return;
		}
		LOG.error("Internal websocket error in connection from " + webSocketSession.getRemoteSocketAddress(), error);
	}

	@OnWebSocketMessage
	public void onMessage(Reader reader) {
		if (webSocketSession == null) {
			LOG.error("Trying to receive message when websocket is disconnected");
			return;
		}
		XtextWebRequest request;
		try {
			request = gson.fromJson(reader, XtextWebRequest.class);
		} catch (JsonIOException e) {
			LOG.error("Cannot read from websocket from " + webSocketSession.getRemoteSocketAddress(), e);
			if (webSocketSession.isOpen()) {
				executor.dispose();
				webSocketSession.close(StatusCode.SERVER_ERROR, "Cannot read payload", Callback.NOOP);
			}
			return;
		} catch (JsonParseException e) {
			LOG.warn("Malformed websocket request from " + webSocketSession.getRemoteSocketAddress(), e);
			if (webSocketSession.isOpen()) {
				executor.dispose();
				webSocketSession.close(XtextStatusCode.INVALID_JSON, "Invalid JSON payload", Callback.NOOP);
			}
			return;
		}
		try {
			executor.handleRequest(request);
		} catch (ResponseHandlerException e) {
			LOG.warn("Cannot write websocket response", e);
			if (webSocketSession.isOpen()) {
				executor.dispose();
				webSocketSession.close(StatusCode.SERVER_ERROR, "Cannot write response", Callback.NOOP);
			}
		}
	}

	@Override
	public void onResponse(XtextWebResponse response) throws ResponseHandlerException {
		if (webSocketSession == null) {
			throw new ResponseHandlerException("Trying to send message when websocket is disconnected");
		}
		var responseString = gson.toJson(response);
		webSocketSession.sendText(responseString, Callback.from(() -> {}, this::writeFailed));
	}

	public void writeFailed(Throwable x) {
		if (webSocketSession == null) {
			LOG.error("Cannot complete async write to disconnected websocket", x);
			return;
		}
		LOG.warn("Cannot complete async write to websocket " + webSocketSession.getRemoteSocketAddress(), x);
	}
}
