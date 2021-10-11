package tools.refinery.language.web.xtext;

import java.io.IOException;
import java.io.Reader;

import org.eclipse.emf.common.util.URI;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.web.server.IServiceContext;
import org.eclipse.xtext.web.server.IServiceResult;
import org.eclipse.xtext.web.server.ISession;
import org.eclipse.xtext.web.server.IUnwrappableServiceResult;
import org.eclipse.xtext.web.server.InvalidRequestException;
import org.eclipse.xtext.web.server.InvalidRequestException.UnknownLanguageException;
import org.eclipse.xtext.web.server.XtextServiceDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.inject.Injector;

@WebSocket
public class XtextWebSocket {
	private static final Logger LOG = LoggerFactory.getLogger(XtextWebSocket.class);

	private final Gson gson = new Gson();

	private final ISession session;

	private final IResourceServiceProvider.Registry resourceServiceProviderRegistry;

	public XtextWebSocket(ISession session, IResourceServiceProvider.Registry resourceServiceProviderRegistry) {
		this.session = session;
		this.resourceServiceProviderRegistry = resourceServiceProviderRegistry;
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
		var serviceContext = new SimpleServiceContext(session, request.getRequestData());
		var response = handleMessage(webSocketSession, serviceContext);
		response.setId(request.getId());
		var responseString = gson.toJson(response);
		try {
			webSocketSession.getRemote().sendPartialString(responseString, true, new WriteCallback() {
				@Override
				public void writeFailed(Throwable x) {
					LOG.warn("Cannot complete async write to websocket " + webSocketSession.getRemoteAddress(), x);
				}
			});
		} catch (IOException e) {
			LOG.warn("Cannot initiaite async write to websocket " + webSocketSession.getRemoteAddress(), e);
			if (webSocketSession.isOpen()) {
				webSocketSession.close(StatusCode.SERVER_ERROR, "Cannot write payload");
			}
		}
	}

	protected XtextWebSocketResponse handleMessage(Session webSocketSession, IServiceContext serviceContext) {
		IServiceResult serviceResult;
		try {
			var injector = getInjector(serviceContext);
			var serviceDispatcher = injector.getInstance(XtextServiceDispatcher.class);
			var service = serviceDispatcher.getService(serviceContext);
			serviceResult = service.getService().apply();
		} catch (InvalidRequestException e) {
			LOG.warn("Invalid request from websocket " + webSocketSession.getRemoteAddress(), e);
			var error = new XtextWebSocketErrorResponse();
			error.setErrorMessage(e.getMessage());
			return error;
		}
		var response = new XtextWebSocketOkResponse();
		if (serviceResult instanceof IUnwrappableServiceResult unwrappableServiceResult
				&& unwrappableServiceResult.getContent() != null) {
			response.setResponseData(unwrappableServiceResult.getContent());
		} else {
			response.setResponseData(serviceResult);
		}
		return response;
	}

	/**
	 * Get the injector to satisfy the request in the {@code serviceContext}.
	 * 
	 * Based on {@link org.eclipse.xtext.web.servlet.XtextServlet#getInjector}.
	 * 
	 * @param serviceContext the Xtext service context of the request
	 * @return the injector for the Xtext language in the request
	 * @throws UnknownLanguageException if the Xtext language cannot be determined
	 */
	protected Injector getInjector(IServiceContext serviceContext) {
		IResourceServiceProvider resourceServiceProvider = null;
		var resourceName = serviceContext.getParameter("resource");
		if (resourceName == null) {
			resourceName = "";
		}
		var emfURI = URI.createURI(resourceName);
		var contentType = serviceContext.getParameter("contentType");
		if (Strings.isNullOrEmpty(contentType)) {
			resourceServiceProvider = resourceServiceProviderRegistry.getResourceServiceProvider(emfURI);
			if (resourceServiceProvider == null) {
				if (emfURI.toString().isEmpty()) {
					throw new UnknownLanguageException(
							"Unable to identify the Xtext language: missing parameter 'resource' or 'contentType'.");
				} else {
					throw new UnknownLanguageException(
							"Unable to identify the Xtext language for resource " + emfURI + ".");
				}
			}
		} else {
			resourceServiceProvider = resourceServiceProviderRegistry.getResourceServiceProvider(emfURI, contentType);
			if (resourceServiceProvider == null) {
				throw new UnknownLanguageException(
						"Unable to identify the Xtext language for contentType " + contentType + ".");
			}
		}
		return resourceServiceProvider.get(Injector.class);
	}
}
