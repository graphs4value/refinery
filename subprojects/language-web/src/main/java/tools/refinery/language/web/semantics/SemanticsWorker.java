/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
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
import tools.refinery.generator.ModelSemantics;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.web.semantics.metadata.MetadataCreator;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.util.CancellationToken;

import java.util.ArrayList;
import java.util.concurrent.Callable;

class SemanticsWorker implements Callable<SemanticsResult> {
	private static final String DIAGNOSTIC_ID = "tools.refinery.language.semantics.SemanticError";

	@Inject
	private OperationCanceledManager operationCanceledManager;

	@Inject
	private IDiagnosticConverter diagnosticConverter;

	@Inject
	private ModelSemanticsFactory semanticsFactory;

	@Inject
	private MetadataCreator metadataCreator;

	@Inject
	private PartialInterpretation2Json partialInterpretation2Json;

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
		cancellationToken.checkCancelled();
		ModelSemantics semantics;
		try {
			semantics = semanticsFactory.cancellationToken(cancellationToken).createSemantics(problem);
		} catch (TranslationException e) {
			return new SemanticsInternalErrorResult(e.getMessage());
		} catch (TracedException e) {
			var cause = e.getCause();
			// Suppress the type of the cause exception.
			var message = cause == null ? e.getMessage() : cause.getMessage();
			return getTracedErrorResult(e.getSourceElement(), message);
		}
		cancellationToken.checkCancelled();
		metadataCreator.setProblemTrace(semantics.getProblemTrace());
		var nodesMetadata = metadataCreator.getNodesMetadata(semantics.getModel(), Concreteness.PARTIAL);
		cancellationToken.checkCancelled();
		var relationsMetadata = metadataCreator.getRelationsMetadata();
		cancellationToken.checkCancelled();
		var partialInterpretation = partialInterpretation2Json.getPartialInterpretation(semantics, cancellationToken);
		return new SemanticsSuccessResult(nodesMetadata, relationsMetadata, partialInterpretation);
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
