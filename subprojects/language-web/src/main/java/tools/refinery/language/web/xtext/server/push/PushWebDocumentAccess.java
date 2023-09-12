/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server.push;

import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.concurrent.CancelableUnitOfWork;
import org.eclipse.xtext.web.server.IServiceResult;
import org.eclipse.xtext.web.server.model.AbstractCachedService;
import org.eclipse.xtext.web.server.model.IXtextWebDocument;
import org.eclipse.xtext.web.server.model.PrecomputedServiceRegistry;
import org.eclipse.xtext.web.server.model.XtextWebDocument;
import org.eclipse.xtext.web.server.model.XtextWebDocumentAccess;
import org.eclipse.xtext.web.server.syntaxcoloring.HighlightingService;
import org.eclipse.xtext.web.server.validation.ValidationService;

import com.google.inject.Inject;
import tools.refinery.language.web.semantics.SemanticsService;

public class PushWebDocumentAccess extends XtextWebDocumentAccess {

	@Inject
	private PrecomputedServiceRegistry preComputedServiceRegistry;

	@Inject
	private OperationCanceledManager operationCanceledManager;

	private PushWebDocument pushDocument;

	@Override
	protected void init(XtextWebDocument document, String requiredStateId, boolean skipAsyncWork) {
		super.init(document, requiredStateId, skipAsyncWork);
		if (document instanceof PushWebDocument newPushDocument) {
			pushDocument = newPushDocument;
		}
	}

	@Override
	protected void performPrecomputation(CancelIndicator cancelIndicator) {
		if (pushDocument == null) {
			super.performPrecomputation(cancelIndicator);
			return;
		}
		for (AbstractCachedService<? extends IServiceResult> service : preComputedServiceRegistry
				.getPrecomputedServices()) {
			operationCanceledManager.checkCanceled(cancelIndicator);
			precomputeServiceResult(service, false);
		}
	}

	protected <T extends IServiceResult> void precomputeServiceResult(AbstractCachedService<T> service, boolean logCacheMiss) {
		var serviceName = getPrecomputedServiceName(service);
		readOnly(new CancelableUnitOfWork<Void, IXtextWebDocument>() {
			@Override
			public java.lang.Void exec(IXtextWebDocument d, CancelIndicator cancelIndicator) throws Exception {
				pushDocument.precomputeServiceResult(service, serviceName, cancelIndicator, logCacheMiss);
				return null;
			}
		});
	}

	protected String getPrecomputedServiceName(AbstractCachedService<? extends IServiceResult> service) {
		if (service instanceof ValidationService) {
			return "validate";
		}
		if (service instanceof HighlightingService) {
			return "highlight";
		}
		if (service instanceof SemanticsService) {
			return "semantics";
		}
		throw new IllegalArgumentException("Unknown precomputed service: " + service);
	}

	public void cancelModelGeneration() {
		pushDocument.cancelModelGeneration();
	}
}
