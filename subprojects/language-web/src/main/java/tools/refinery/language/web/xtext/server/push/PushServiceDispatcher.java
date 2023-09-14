/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server.push;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.eclipse.xtext.web.server.IServiceContext;
import org.eclipse.xtext.web.server.InvalidRequestException;
import org.eclipse.xtext.web.server.XtextServiceDispatcher;
import org.eclipse.xtext.web.server.model.PrecomputedServiceRegistry;
import org.eclipse.xtext.web.server.model.XtextWebDocument;

import com.google.inject.Singleton;

import tools.refinery.language.web.generator.ModelGenerationService;
import tools.refinery.language.web.semantics.SemanticsService;
import tools.refinery.language.web.xtext.server.SubscribingServiceContext;

@Singleton
public class PushServiceDispatcher extends XtextServiceDispatcher {
	@Inject
	private SemanticsService semanticsService;

	@Inject
	private ModelGenerationService modelGenerationService;

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

	@Override
	protected ServiceDescriptor createServiceDescriptor(String serviceType, IServiceContext context) {
		if (ModelGenerationService.SERVICE_NAME.equals(serviceType)) {
			return getModelGenerationService(context);
		}
		return super.createServiceDescriptor(serviceType, context);
	}

	protected ServiceDescriptor getModelGenerationService(IServiceContext context) throws InvalidRequestException {
		var document = (PushWebDocumentAccess) getDocumentAccess(context);
		// Using legacy Guava methods because of the Xtext dependency.
		@SuppressWarnings({"Guava", "squid:S4738"})
		boolean start = getBoolean(context, "start", Optional.of(false));
		@SuppressWarnings({"Guava", "squid:S4738"})
		boolean cancel = getBoolean(context, "cancel", Optional.of(false));
		if (!start && !cancel) {
			throw new InvalidRequestException("Either start of cancel must be specified");
		}
		@SuppressWarnings({"squid:S4738"})
		int randomSeed = start ? getInt(context, "randomSeed", Optional.absent()) : 0;
		var descriptor = new ServiceDescriptor();
		descriptor.setService(() -> {
			try {
				if (start) {
					return modelGenerationService.generateModel(document, randomSeed);
				} else {
					return modelGenerationService.cancelModelGeneration(document);
				}
			} catch (RuntimeException e) {
				return handleError(descriptor, e);
			}
		});
		return descriptor;
	}
}
