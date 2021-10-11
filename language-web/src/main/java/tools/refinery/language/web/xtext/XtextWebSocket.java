package tools.refinery.language.web.xtext;

import java.io.IOException;
import java.io.Reader;

import org.eclipse.emf.common.util.URI;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.web.server.IServiceContext;
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
public class XtextWebSocket implements WriteCallback {
	private static final Logger LOG = LoggerFactory.getLogger(XtextWebSocket.class);

	private final Gson gson = new Gson();

	private final ISession session;

	private final IResourceServiceProvider.Registry resourceServiceProviderRegistry;

	public XtextWebSocket(ISession session, IResourceServiceProvider.Registry resourceServiceProviderRegistry) {
		this.session = session;
		this.resourceServiceProviderRegistry = resourceServiceProviderRegistry;
	}

	@OnWebSocketMessage
	public void onMessage(Session webSocketSession, Reader reader) {
		XtextWebSocketRequest request;
		try {
			request = gson.fromJson(reader, XtextWebSocketRequest.class);
		} catch (JsonIOException e) {
			LOG.error("Cannot read from websocket " + webSocketSession.getRemoteAddress(), e);
			if (webSocketSession.isOpen()) {
				webSocketSession.close(StatusCode.SERVER_ERROR, "Cannot read payload");
			}
			return;
		} catch (JsonParseException e) {
			LOG.warn("Malformed websocket request from " + webSocketSession.getRemoteAddress(), e);
			webSocketSession.close(XtextStatusCode.INVALID_JSON, "Invalid JSON payload");
			return;
		}
		var serviceContext = new SimpleServiceContext(session, request.getRequestData());
		var response = handleMessage(serviceContext);
		response.setId(request.getId());
		var responseString = gson.toJson(response);
		try {
			webSocketSession.getRemote().sendPartialString(responseString, true, this);
		} catch (IOException e) {
			LOG.warn("Cannot initiaite async write to websocket to " + webSocketSession.getRemoteAddress(), e);
			if (webSocketSession.isOpen()) {
				webSocketSession.close(StatusCode.SERVER_ERROR, "Cannot write payload");
			}
		}
	}

	@Override
	public void writeFailed(Throwable x) {
		LOG.warn("Cannot complete async write to websocket", x);
	}

	protected XtextWebSocketResponse handleMessage(IServiceContext serviceContext) {
		try {
			var injector = getInjector(serviceContext);
			var serviceDispatcher = injector.getInstance(XtextServiceDispatcher.class);
			var service = serviceDispatcher.getService(serviceContext);
			var serviceResult = service.getService().apply();
			var response = new XtextWebSocketOkResponse();
			if (serviceResult instanceof IUnwrappableServiceResult unwrappableServiceResult
					&& unwrappableServiceResult.getContent() != null) {
				response.setResponseData(unwrappableServiceResult.getContent());
			} else {
				response.setResponseData(serviceResult);
			}
			return response;
		} catch (InvalidRequestException e) {
			LOG.warn("Invalid request", e);
			var error = new XtextWebSocketErrorResponse();
			error.setErrorMessage(e.getMessage());
			return error;
		}
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
