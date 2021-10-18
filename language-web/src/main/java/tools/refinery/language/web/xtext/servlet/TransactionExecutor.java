package tools.refinery.language.web.xtext.servlet;

import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.web.server.IServiceResult;
import org.eclipse.xtext.web.server.ISession;
import org.eclipse.xtext.web.server.InvalidRequestException;
import org.eclipse.xtext.web.server.InvalidRequestException.UnknownLanguageException;
import org.eclipse.xtext.web.server.ServiceConflictResult;
import org.eclipse.xtext.web.server.XtextServiceDispatcher;
import org.eclipse.xtext.web.server.contentassist.ContentAssistResult;
import org.eclipse.xtext.web.server.formatting.FormattingResult;
import org.eclipse.xtext.web.server.hover.HoverResult;
import org.eclipse.xtext.web.server.model.DocumentStateResult;
import org.eclipse.xtext.web.server.occurrences.OccurrencesResult;
import org.eclipse.xtext.web.server.persistence.ResourceContentResult;

import com.google.common.base.Strings;
import com.google.inject.Injector;

public class TransactionExecutor {
	private final ISession session;

	private final IResourceServiceProvider.Registry resourceServiceProviderRegistry;

	public TransactionExecutor(ISession session, IResourceServiceProvider.Registry resourceServiceProviderRegistry) {
		this.session = session;
		this.resourceServiceProviderRegistry = resourceServiceProviderRegistry;
	}

	public void handleRequest(XtextWebSocketRequest request, ResponseHandler handler) throws IOException {
		var requestData = request.getRequestData();
		if (requestData == null || requestData.isEmpty()) {
			// Nothing to do.
			return;
		}
		int nCalls = requestData.size();
		int lastCall = handleTransaction(request, handler);
		for (int index = lastCall + 1; index < nCalls; index++) {
			handler.onResponse(
					new XtextWebSocketErrorResponse(request, index, XtextWebSocketErrorKind.TRANSACTION_CANCELLED));
		}
	}

	protected int handleTransaction(XtextWebSocketRequest request, ResponseHandler handler) throws IOException {
		var requestData = request.getRequestData();
		var stateId = request.getRequiredStateId();
		int index = 0;
		try {
			var injector = getInjector(request);
			var serviceDispatcher = injector.getInstance(XtextServiceDispatcher.class);
			int nCalls = requestData.size();
			for (; index < nCalls; index++) {
				var serviceContext = SimpleServiceContext.ofTransaction(session, request, stateId, index);
				var service = serviceDispatcher.getService(serviceContext);
				var serviceResult = service.getService().apply();
				handler.onResponse(new XtextWebSocketOkResponse(request, index, serviceResult));
				if (serviceResult instanceof ServiceConflictResult) {
					break;
				}
				var nextStateId = getNextStateId(serviceResult);
				if (nextStateId != null) {
					stateId = nextStateId;
				}
			}
		} catch (InvalidRequestException e) {
			handler.onResponse(
					new XtextWebSocketErrorResponse(request, index, XtextWebSocketErrorKind.REQUEST_ERROR, e));
		} catch (RuntimeException e) {
			handler.onResponse(
					new XtextWebSocketErrorResponse(request, index, XtextWebSocketErrorKind.SERVER_ERROR, e));
		}
		return index;
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
	protected Injector getInjector(XtextWebSocketRequest request) {
		IResourceServiceProvider resourceServiceProvider = null;
		var resourceName = request.getResourceName();
		if (resourceName == null) {
			resourceName = "";
		}
		var emfURI = URI.createURI(resourceName);
		var contentType = request.getContentType();
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

	protected String getNextStateId(IServiceResult serviceResult) {
		if (serviceResult instanceof ContentAssistResult contentAssistResult) {
			return contentAssistResult.getStateId();
		}
		if (serviceResult instanceof DocumentStateResult documentStateResult) {
			return documentStateResult.getStateId();
		}
		if (serviceResult instanceof FormattingResult formattingResult) {
			return formattingResult.getStateId();
		}
		if (serviceResult instanceof HoverResult hoverResult) {
			return hoverResult.getStateId();
		}
		if (serviceResult instanceof OccurrencesResult occurrencesResult) {
			return occurrencesResult.getStateId();
		}
		if (serviceResult instanceof ResourceContentResult resourceContentResult) {
			return resourceContentResult.getStateId();
		}
		return null;
	}
}
