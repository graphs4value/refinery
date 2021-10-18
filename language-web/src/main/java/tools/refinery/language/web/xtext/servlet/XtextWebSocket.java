package tools.refinery.language.web.xtext.servlet;

import java.io.IOException;
import java.io.Reader;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.web.server.ISession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;

@WebSocket
public class XtextWebSocket {
	private static final Logger LOG = LoggerFactory.getLogger(XtextWebSocket.class);

	private final Gson gson = new Gson();

	private final TransactionExecutor executor;

	public XtextWebSocket(TransactionExecutor executor) {
		this.executor = executor;
	}

	public XtextWebSocket(ISession session, IResourceServiceProvider.Registry resourceServiceProviderRegistry) {
		this(new TransactionExecutor(session, resourceServiceProviderRegistry));
	}

	@OnWebSocketConnect
	public void onConnect(Session webSocketSession) {
		LOG.debug("New websocket connection from {}", webSocketSession.getRemoteAddress());
	}

	@OnWebSocketClose
	public void onClose(Session webSocketSession, int statusCode, String reason) {
		if (statusCode == StatusCode.NORMAL) {
			LOG.debug("{} closed connection normally: {}", webSocketSession.getRemoteAddress(), reason);
		} else {
			LOG.warn("{} closed connection with status code {}: {}", webSocketSession.getRemoteAddress(), statusCode,
					reason);
		}
	}

	@OnWebSocketError
	public void onError(Session webSocketSession, Throwable error) {
		LOG.error("Internal websocket error in connection from" + webSocketSession.getRemoteAddress(), error);
	}

	@OnWebSocketMessage
	public void onMessage(Session webSocketSession, Reader reader) {
		XtextWebSocketRequest request;
		try {
			request = gson.fromJson(reader, XtextWebSocketRequest.class);
		} catch (JsonIOException e) {
			LOG.error("Cannot read from websocket from" + webSocketSession.getRemoteAddress(), e);
			if (webSocketSession.isOpen()) {
				webSocketSession.close(StatusCode.SERVER_ERROR, "Cannot read payload");
			}
			return;
		} catch (JsonParseException e) {
			LOG.warn("Malformed websocket request from" + webSocketSession.getRemoteAddress(), e);
			webSocketSession.close(XtextStatusCode.INVALID_JSON, "Invalid JSON payload");
			return;
		}
		try {
			executor.handleRequest(request, response -> sendResponse(webSocketSession, response));
		} catch (IOException e) {
			LOG.warn("Cannot initiaite async write to websocket " + webSocketSession.getRemoteAddress(), e);
			if (webSocketSession.isOpen()) {
				webSocketSession.close(StatusCode.SERVER_ERROR, "Cannot write payload");
			}
		}
	}

	protected void sendResponse(Session webSocketSession, XtextWebSocketResponse response) throws IOException {
		var responseString = gson.toJson(response);
		webSocketSession.getRemote().sendPartialString(responseString, true, new WriteCallback() {
			@Override
			public void writeFailed(Throwable x) {
				LOG.warn("Cannot complete async write to websocket " + webSocketSession.getRemoteAddress(), x);
			}
		});
	}
}
