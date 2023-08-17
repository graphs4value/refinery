/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server.push;

import com.google.inject.Inject;
import org.eclipse.xtext.web.server.IServiceContext;
import org.eclipse.xtext.web.server.XtextServiceDispatcher;
import org.eclipse.xtext.web.server.model.PrecomputedServiceRegistry;
import org.eclipse.xtext.web.server.model.XtextWebDocument;

import com.google.inject.Singleton;

import tools.refinery.language.web.semantics.SemanticsService;
import tools.refinery.language.web.xtext.server.SubscribingServiceContext;

@Singleton
public class PushServiceDispatcher extends XtextServiceDispatcher {
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
}
