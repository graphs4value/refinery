/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.util.IDisposable;
import org.eclipse.xtext.web.server.*;
import org.eclipse.xtext.web.server.InvalidRequestException.UnknownLanguageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.language.web.xtext.server.message.*;
import tools.refinery.language.web.xtext.server.push.PrecomputationListener;
import tools.refinery.language.web.xtext.server.push.PushWebDocument;
import tools.refinery.language.web.xtext.servlet.SimpleServiceContext;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionExecutor implements IDisposable, PrecomputationListener {
	private static final Logger LOG = LoggerFactory.getLogger(TransactionExecutor.class);

	private final ISession session;

	private final IResourceServiceProvider.Registry resourceServiceProviderRegistry;

	private final Map<String, WeakReference<PushWebDocument>> subscriptions = new HashMap<>();

	private ResponseHandler responseHandler;

	private final Object callPendingLock = new Object();

	private boolean callPending;

	private final List<XtextWebPushMessage> pendingPushMessages = new ArrayList<>();

	private volatile boolean disposed;

	public TransactionExecutor(ISession session, IResourceServiceProvider.Registry resourceServiceProviderRegistry) {
		this.session = session;
		this.resourceServiceProviderRegistry = resourceServiceProviderRegistry;
	}

	public void setResponseHandler(ResponseHandler responseHandler) {
		this.responseHandler = responseHandler;
	}

	public void handleRequest(XtextWebRequest request) throws ResponseHandlerException {
		if (disposed) {
			return;
		}
		var serviceContext = new SimpleServiceContext(session, request.getRequestData());
		var ping = serviceContext.getParameter("ping");
		if (ping != null) {
			onResponse(new XtextWebOkResponse(request, new PongResult(ping)));
			return;
		}
		synchronized (callPendingLock) {
			if (callPending) {
				LOG.error("Reentrant request detected");
			}
			if (!pendingPushMessages.isEmpty()) {
				LOG.error("{} push messages got stuck without a pending request", pendingPushMessages.size());
			}
			callPending = true;
		}
		try {
			var injector = getInjector(serviceContext);
			var serviceDispatcher = injector.getInstance(XtextServiceDispatcher.class);
			var service = serviceDispatcher.getService(new SubscribingServiceContext(serviceContext, this));
			var serviceResult = service.getService().apply();
			onResponse(new XtextWebOkResponse(request, serviceResult));
		} catch (InvalidRequestException e) {
			onResponse(new XtextWebErrorResponse(request, XtextWebErrorKind.REQUEST_ERROR, e));
		} catch (RuntimeException e) {
			onResponse(new XtextWebErrorResponse(request, XtextWebErrorKind.SERVER_ERROR, e));
		} finally {
			flushPendingPushMessages();
		}
	}

	private void onResponse(XtextWebResponse response) throws ResponseHandlerException {
		if (!disposed) {
			responseHandler.onResponse(response);
		}
	}

	private void flushPendingPushMessages() {
		synchronized (callPendingLock) {
			for (var message : pendingPushMessages) {
				if (disposed) {
					return;
				}
				try {
					responseHandler.onResponse(message);
				} catch (ResponseHandlerException | RuntimeException e) {
					LOG.error("Error while flushing push message", e);
				}
			}
			pendingPushMessages.clear();
			callPending = false;
		}
	}

	@Override
	public void onPrecomputedServiceResult(String resourceId, String stateId, String serviceName,
			IServiceResult serviceResult) throws ResponseHandlerException {
		var message = new XtextWebPushMessage(resourceId, stateId, serviceName, serviceResult);
		synchronized (callPendingLock) {
			// If we're currently responding to a call we must delay any push messages until
			// the reply is sent, because push messages relating to the new state id must be
			// sent after the response with the new state id so that the client knows about
			// the new state when it receives the push message.
			if (callPending) {
				pendingPushMessages.add(message);
			} else {
				responseHandler.onResponse(message);
			}
		}
	}

	@Override
	public void onSubscribeToPrecomputationEvents(String resourceId, PushWebDocument document) {
		PushWebDocument previousDocument = null;
		var previousSubscription = subscriptions.get(resourceId);
		if (previousSubscription != null) {
			previousDocument = previousSubscription.get();
		}
		if (previousDocument == document) {
			return;
		}
		if (previousDocument != null) {
			previousDocument.removePrecomputationListener(this);
		}
		subscriptions.put(resourceId, new WeakReference<>(document));
	}

	/**
	 * Get the injector to satisfy the request in the {@code serviceContext}.
	 * Based on {@link org.eclipse.xtext.web.servlet.XtextServlet#getInjector}.
	 *
	 * @param context the Xtext service context of the request
	 * @return the injector for the Xtext language in the request
	 * @throws UnknownLanguageException if the Xtext language cannot be determined
	 */
	protected Injector getInjector(IServiceContext context) {
		IResourceServiceProvider resourceServiceProvider;
		var resourceName = context.getParameter("resource");
		if (resourceName == null) {
			resourceName = "";
		}
		var emfURI = URI.createURI(resourceName);
		var contentType = context.getParameter("contentType");
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

	@Override
	public void dispose() {
		disposed = true;
		for (var subscription : subscriptions.values()) {
			var document = subscription.get();
			if (document != null) {
				document.removePrecomputationListener(this);
				document.dispose();
			}
		}
	}
}
