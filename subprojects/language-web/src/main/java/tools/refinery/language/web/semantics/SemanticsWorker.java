/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import com.google.inject.Inject;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckType;
import org.eclipse.xtext.validation.FeatureBasedDiagnostic;
import org.eclipse.xtext.validation.IDiagnosticConverter;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.web.server.validation.ValidationResult;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.metadata.MetadataCreator;
import tools.refinery.language.semantics.model.ModelInitializer;
import tools.refinery.language.semantics.model.TracedException;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.util.CancellationToken;

import java.util.ArrayList;
import java.util.concurrent.Callable;

class SemanticsWorker implements Callable<SemanticsResult> {
	private static final String DIAGNOSTIC_ID = "tools.refinery.language.semantics.SemanticError";

	@Inject
	private PartialInterpretation2Json partialInterpretation2Json;

	@Inject
	private OperationCanceledManager operationCanceledManager;

	@Inject
	private IDiagnosticConverter diagnosticConverter;

	@Inject
	private ModelInitializer initializer;

	@Inject
	private MetadataCreator metadataCreator;

	private Problem problem;

	private CancellationToken cancellationToken;

	public void setProblem(Problem problem, CancelIndicator parentIndicator) {
		this.problem = problem;
		cancellationToken = () -> {
			if (Thread.interrupted() || parentIndicator.isCanceled()) {
				operationCanceledManager.throwOperationCanceledException();
			}
		};
	}

	@Override
	public SemanticsResult call() {
		var builder = ModelStore.builder()
				.cancellationToken(cancellationToken)
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(Concreteness.PARTIAL));
		cancellationToken.checkCancelled();
		try {
			var modelSeed = initializer.createModel(problem, builder);
			cancellationToken.checkCancelled();
			metadataCreator.setInitializer(initializer);
			cancellationToken.checkCancelled();
			var nodesMetadata = metadataCreator.getNodesMetadata();
			cancellationToken.checkCancelled();
			var relationsMetadata = metadataCreator.getRelationsMetadata();
			cancellationToken.checkCancelled();
			var store = builder.build();
			cancellationToken.checkCancelled();
			var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
			cancellationToken.checkCancelled();
			var partialInterpretation = partialInterpretation2Json.getPartialInterpretation(initializer, model,
					Concreteness.PARTIAL, cancellationToken);

			return new SemanticsSuccessResult(nodesMetadata, relationsMetadata, partialInterpretation);
		} catch (TracedException e) {
			return getTracedErrorResult(e.getSourceElement(), e.getMessage());
		} catch (TranslationException e) {
			var sourceElement = initializer.getInverseTrace(e.getPartialSymbol());
			return getTracedErrorResult(sourceElement, e.getMessage());
		}
	}

	private SemanticsResult getTracedErrorResult(EObject sourceElement, String message) {
		if (sourceElement == null || !problem.eResource().equals(sourceElement.eResource())) {
			return new SemanticsInternalErrorResult(message);
		}
		var diagnostic = new FeatureBasedDiagnostic(Diagnostic.ERROR, message, sourceElement, null, 0,
				CheckType.EXPENSIVE, DIAGNOSTIC_ID);
		var xtextIssues = new ArrayList<Issue>();
		diagnosticConverter.convertValidatorDiagnostic(diagnostic, xtextIssues::add);
		var issues = xtextIssues.stream()
				.map(issue -> new ValidationResult.Issue(issue.getMessage(), "error", issue.getLineNumber(),
						issue.getColumn(), issue.getOffset(), issue.getLength()))
				.toList();
		return new SemanticsIssuesResult(issues);
	}
}
