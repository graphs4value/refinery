/*
 * Copyright (c) 2015, 2020 itemis AG (http://www.itemis.eu) and others.
 * Copyright (c) 2021-2024 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server.push;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.xtext.web.server.IServiceContext;
import org.eclipse.xtext.web.server.InvalidRequestException;
import org.eclipse.xtext.web.server.XtextServiceDispatcher;
import org.eclipse.xtext.web.server.model.PrecomputedServiceRegistry;
import org.eclipse.xtext.web.server.model.UpdateDocumentService;
import org.eclipse.xtext.web.server.model.XtextWebDocument;
import org.eclipse.xtext.web.server.model.XtextWebDocumentAccess;
import tools.refinery.language.web.semantics.SemanticsService;
import tools.refinery.language.web.xtext.server.SubscribingServiceContext;

@Singleton
public class PushServiceDispatcher extends XtextServiceDispatcher {
	@Inject
	private UpdateDocumentService updateDocumentService;

	@Inject
	private XtextWebDocumentAccess.Factory documentAccessFactory;

	@Inject
	private SemanticsService semanticsService;

	@Override
	@Inject
	protected void registerPreComputedServices(PrecomputedServiceRegistry registry) {
		super.registerPreComputedServices(registry);
		registry.addPrecomputedService(semanticsService);
	}

	@Override
	protected XtextWebDocument getFullTextDocument(String fullText, String resourceId, IServiceContext context) {
		var document = super.getFullTextDocument(fullText, resourceId, context);
		if (document instanceof PushWebDocument pushWebDocument
				&& context instanceof SubscribingServiceContext subscribingContext) {
			pushWebDocument.addPrecomputationListener(subscribingContext.subscriber());
		}
		return document;
	}

	/**
	 * This method is based on {@link XtextServiceDispatcher#getUpdateDocumentService(IServiceContext)}, but it was
	 * modified to
	 *
	 * @param context The service context.
	 * @return The service descriptor corresponding to the requested document update.
	 * @throws InvalidRequestException If the request fails to process.
	 */
	@Override
	// We didn't simplify the cyclomatic complexity of the method to keep the structure of the original for easy
	// synchronization with upstream. The use of {@code Optional} and {@code Throwable} in this method also follows
	// the original one.
	@SuppressWarnings({"squid:S1181", "squid:S3776", "squid:S4738"})
	protected ServiceDescriptor getUpdateDocumentService(IServiceContext context) throws InvalidRequestException {
		String resourceId = getResourceID(context);
		if (resourceId == null) {
			throw new InvalidRequestException.InvalidParametersException("The parameter 'resource' is required.");
		}
		var concretize = PushWebDocumentProvider.getConcretize(context);
		String fullText = context.getParameter("fullText");
		XtextWebDocument document = getResourceDocument(resourceId, context);
		boolean initializedFromFullText = (document == null);
		if (initializedFromFullText) {
			if (fullText == null) {
				throw new InvalidRequestException.ResourceNotFoundException("The requested resource was not found.");
			}
			document = getFullTextDocument(fullText, resourceId, context);
		}
		var pushDocument = ((PushWebDocument) document);
		XtextWebDocumentAccess documentAccess = documentAccessFactory.create(document,
				context.getParameter("requiredStateId"), false);
		ServiceDescriptor serviceDescriptor = new ServiceDescriptor();
		serviceDescriptor.setHasSideEffects(true);
		if (fullText == null) {
			String deltaText = context.getParameter("deltaText");
			if (deltaText == null) {
				throw new InvalidRequestException.InvalidParametersException(
						"One of the parameters 'deltaText' and 'fullText' must be specified.");
			}
			int deltaOffset = getInt(context, "deltaOffset", Optional.absent());
			if (deltaOffset < 0) {
				throw new InvalidRequestException.InvalidParametersException(
						"The parameter 'deltaOffset' must not be negative.");
			}
			int deltaReplaceLength = getInt(context, "deltaReplaceLength", Optional.absent());
			if (deltaReplaceLength < 0) {
				throw new InvalidRequestException.InvalidParametersException(
						"The parameter 'deltaReplaceLength' must not be negative.");
			}
			serviceDescriptor.setService(() -> {
				try {
					concretize.ifPresent(pushDocument::setConcretize);
					return updateDocumentService.updateDeltaText(documentAccess, deltaText, deltaOffset,
							deltaReplaceLength);
				} catch (Throwable throwable) {
					return handleError(serviceDescriptor, throwable);
				}
			});
		} else {
			if (context.getParameterKeys().contains("deltaText")) {
				throw new InvalidRequestException.InvalidParametersException(
						"The parameters 'deltaText' and 'fullText' cannot be set in the same request.");
			}
			serviceDescriptor.setService(() -> {
				try {
					if (initializedFromFullText) {
						return updateDocumentService.getStateId(documentAccess);
					} else {
						concretize.ifPresent(pushDocument::setConcretize);
						return updateDocumentService.updateFullText(documentAccess, fullText);
					}
				} catch (Throwable throwable) {
					return handleError(serviceDescriptor, throwable);
				}
			});
		}
		return serviceDescriptor;
	}
}
