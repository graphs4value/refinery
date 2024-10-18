/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.semantics;

import com.google.inject.Inject;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.service.OperationCanceledManager;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckType;
import org.eclipse.xtext.validation.FeatureBasedDiagnostic;
import org.eclipse.xtext.validation.IDiagnosticConverter;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.web.server.validation.ValidationResult;
import tools.refinery.generator.ModelFacadeResult;
import tools.refinery.generator.ModelSemantics;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemPackage;
import tools.refinery.language.model.problem.ScopeDeclaration;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.reasoning.scope.ScopePropagator;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.util.CancellationToken;

import java.util.ArrayList;
import java.util.List;
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
	private PartialInterpretation2Json partialInterpretation2Json;

	private Problem problem;
	private boolean concretize;
	private CancellationToken cancellationToken;

	public void setProblem(Problem problem, boolean concretize, CancelIndicator parentIndicator) {
		this.problem = problem;
		this.concretize = concretize;
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
			semantics = semanticsFactory
					.cancellationToken(cancellationToken)
					.keepNonExistingObjects(true)
					.concretize(concretize)
					.tryCreateSemantics(problem);
		} catch (TranslationException e) {
			return new SemanticsResult(e.getMessage());
		} catch (TracedException e) {
			var cause = e.getCause();
			// Suppress the type of the cause exception.
			var message = cause == null ? e.getMessage() : cause.getMessage();
			return getTracedErrorResult(e.getSourceElement(), message);
		}
		cancellationToken.checkCancelled();
		var modelResult = createSemanticsModelResult(semantics);
		return createSemanticsResult(modelResult, semantics.getProblemTrace(), semantics.getInitializationResult());
	}

	private SemanticsResult getTracedErrorResult(EObject sourceElement, String message) {
		var diagnostics = getTracedDiagnostics(sourceElement, null, message, Diagnostic.ERROR);
		return getSemanticsResultWithDiagnostics(null, message, diagnostics);
	}

	private List<FeatureBasedDiagnostic> getTracedDiagnostics(
			EObject sourceElement, EStructuralFeature feature, String message, int severity) {
		if (sourceElement == null || !problem.eResource().equals(sourceElement.eResource())) {
			return List.of();
		}
		var diagnostic = new FeatureBasedDiagnostic(severity, message, sourceElement, feature, 0,
				CheckType.EXPENSIVE, DIAGNOSTIC_ID);
		return List.of(diagnostic);
	}

	private SemanticsResult getSemanticsResultWithDiagnostics(
			SemanticsModelResult modelResult, String message, List<FeatureBasedDiagnostic> diagnostics) {
		if (diagnostics.isEmpty()) {
			return new SemanticsResult(modelResult, message);
		}
		var issues = convertIssues(diagnostics);
		return new SemanticsResult(modelResult, issues);
	}

	private List<ValidationResult.Issue> convertIssues(List<FeatureBasedDiagnostic> diagnostics) {
		var xtextIssues = new ArrayList<Issue>();
		for (var diagnostic : diagnostics) {
			diagnosticConverter.convertValidatorDiagnostic(diagnostic, xtextIssues::add);
		}
		return xtextIssues.stream()
				.map(issue -> new ValidationResult.Issue(issue.getMessage(), translateSeverity(issue.getSeverity()),
						issue.getLineNumber(), issue.getColumn(), issue.getOffset(), issue.getLength()))
				.toList();
	}

	private String translateSeverity(Severity severity) {
		return switch (severity) {
			case Severity.WARNING -> "warning";
			case Severity.ERROR -> "error";
			case Severity.INFO -> "info";
			case null, default -> "ignore";
		};
	}

	private SemanticsModelResult createSemanticsModelResult(ModelSemantics semantics) {
		var nodesMetadata = semantics.getNodesMetadata();
		cancellationToken.checkCancelled();
		var relationsMetadata = semantics.getRelationsMetadata();
		cancellationToken.checkCancelled();
		var partialInterpretation = partialInterpretation2Json.getPartialInterpretation(semantics, cancellationToken);
		return new SemanticsModelResult(nodesMetadata.list(), relationsMetadata, partialInterpretation);
	}

	private SemanticsResult createSemanticsResult(
			SemanticsModelResult modelResult, ProblemTrace trace, ModelFacadeResult result) {
		if (!(result instanceof ModelFacadeResult.Rejected rejectedResult)) {
			return new SemanticsResult(modelResult);
		}
		var message = rejectedResult.formatMessage();
		int severity = rejectedResult.isPropagationRejected() ? Diagnostic.ERROR : Diagnostic.INFO;
		List<FeatureBasedDiagnostic> diagnostics = switch (rejectedResult.reason()) {
			case MultiObjectTranslator ignored -> getScopePropagatorDiagnostics(message, severity);
			case ScopePropagator ignored -> getScopePropagatorDiagnostics(message, severity);
			case Rule rule -> getRuleDiagnostics(rule, trace, message, severity);
			default -> List.of();
		};
		return getSemanticsResultWithDiagnostics(modelResult, message, diagnostics);
	}

	private List<FeatureBasedDiagnostic> getScopePropagatorDiagnostics(String message, int severity) {
		return problem.getStatements().stream()
				.filter(ScopeDeclaration.class::isInstance)
				.map(eObject -> new FeatureBasedDiagnostic(severity, message, eObject, null, 0,
						CheckType.EXPENSIVE, DIAGNOSTIC_ID))
				.toList();
	}

	private List<FeatureBasedDiagnostic> getRuleDiagnostics(Rule rule, ProblemTrace trace, String message,
															int severity) {
		var ruleDefinition = trace.getInverseRuleDefinitionTrace().get(rule);
		if (ruleDefinition == null) {
			return List.of();
		}
		return getTracedDiagnostics(ruleDefinition, ProblemPackage.Literals.NAMED_ELEMENT__NAME, message, severity);
	}
}
