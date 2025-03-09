/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.web.server.model.AbstractCachedService;
import org.eclipse.xtext.web.server.model.IXtextWebDocument;
import org.eclipse.xtext.web.server.validation.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.language.web.api.ConcretizeWorker;
import tools.refinery.language.web.api.SemanticsWorker;
import tools.refinery.language.web.api.dto.*;
import tools.refinery.language.web.xtext.server.push.PushWebDocument;

@Singleton
public class SemanticsService extends AbstractCachedService<RefineryResponse> {
	private static final Logger LOG = LoggerFactory.getLogger(SemanticsService.class);

	@Inject
	private Provider<SemanticsWorker> semanticsWorkerProvider;

	@Inject
	private Provider<ConcretizeWorker> contretizeWorkerProvider;

	@Inject
	private ValidationService validationService;

	@Override
	public RefineryResponse compute(IXtextWebDocument doc, CancelIndicator cancelIndicator) {
		try {
			if (!(doc instanceof PushWebDocument pushDoc)) {
				throw new IllegalArgumentException("Unexpected IXtextWebDocument: " + doc);
			}
			if (hasError(pushDoc, cancelIndicator)) {
				return null;
			}
			boolean concretize = pushDoc.isConcretize();
			var request = getSemanticsRequest(doc, concretize);
			var worker = concretize ? contretizeWorkerProvider.get() : semanticsWorkerProvider.get();
			var sink = new PrecomputedServiceSink(cancelIndicator);
			worker.schedule(request, sink);
			return sink.getResponse(worker);
		} catch (RuntimeException e) {
			LOG.error("Failed to compute semantics", e);
			return null;
		}
	}

	private boolean hasError(PushWebDocument pushDoc, CancelIndicator cancelIndicator) {
		var validationResult = pushDoc.getCachedServiceResult(validationService, cancelIndicator, true);
		return validationResult.getIssues().stream()
				.anyMatch(issue -> "error".equals(issue.getSeverity()));
	}

	private static SemanticsRequest getSemanticsRequest(IXtextWebDocument doc, boolean concretize) {
		var request = new SemanticsRequest();
		var input = new ProblemInput();
		input.setSource(doc.getText());
		request.setInput(input);
		var outputFormats = new OutputFormats();
		var jsonFormat = new JsonOutputFormat();
		jsonFormat.setEnabled(true);
		jsonFormat.setNonExistingObjects(PartialInterpretationPreservation.KEEP);
		jsonFormat.setShadowPredicates(PartialInterpretationPreservation.KEEP);
		outputFormats.setJson(jsonFormat);
		var sourceFormat = new SourceOutputFormat();
		sourceFormat.setEnabled(concretize);
		outputFormats.setSource(sourceFormat);
		request.setFormat(outputFormats);
		return request;
	}
}
