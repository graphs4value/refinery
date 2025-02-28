/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.validation.CheckType;
import org.eclipse.xtext.validation.FeatureBasedDiagnostic;
import org.eclipse.xtext.validation.IDiagnosticConverter;
import org.eclipse.xtext.validation.Issue;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.RuleDefinition;
import tools.refinery.language.model.problem.ScopeDeclaration;
import tools.refinery.language.model.problem.Statement;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.store.dse.propagation.PropagationRejectedException;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.reasoning.scope.ScopePropagator;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;

import java.util.List;

@Singleton
public class RefineryDiagnostics {
	private static final String PREFIX = "tools.refinery.generator.GeneratorDiagnostics.";
	public static final String TRANSLATION_ERROR = PREFIX + "TRANSLATION_ERROR";
	public static final String PROPAGATION_ERROR = PREFIX + "PROPAGATION_ERROR";
	public static final String CONCRETIZATION_ERROR = PREFIX + "CONCRETIZATION_ERROR";

	@Inject
	private IDiagnosticConverter diagnosticConverter;

	public InvalidProblemException wrapTranslationException(TranslationException e, ProblemTrace trace) {
		var wrappedByTrace = trace.wrapException(e);
		if (wrappedByTrace instanceof TracedException tracedException) {
			return wrapTracedException(tracedException);
		}
		var problem = trace.getProblem();
		var problemUri = problem.eResource().getURI();
		return createInvalidProblemException(problemUri, List.of(), e.getMessage(), e);
	}

	public InvalidProblemException wrapTracedException(TracedException e) {
		var sourceElement = e.getSourceElement();
		var problemUri = sourceElement.eResource().getURI();
		return createInvalidProblemException(problemUri, sourceElement, e);
	}

	private InvalidProblemException createInvalidProblemException(
			URI resourceUri, EObject sourceElement, Throwable cause) {
		return createInvalidProblemException(resourceUri, List.of(sourceElement), cause.getMessage(), cause);
	}

	private InvalidProblemException createInvalidProblemException(
			URI resourceUri, List<? extends EObject> sourceElements, String message, Throwable cause) {
		var issues = convertIssues(sourceElements, message, Diagnostic.ERROR, TRANSLATION_ERROR);
		return new InvalidProblemException(resourceUri, issues, cause);
	}

	public InvalidProblemException createModelFacadeResultException(ModelFacadeResult.Rejected result,
																	ProblemTrace trace) {
		var resourceUri = trace.getProblem().eResource().getURI();
		var issues = convertIssues(result, trace);
		return new InvalidProblemException(resourceUri, issues, null);
	}

	public List<Issue> convertIssues(ModelFacadeResult.Rejected result, ProblemTrace trace) {
		return switch (result) {
			case ModelFacadeResult.PropagationRejected propagationRejected ->
					convertIssues(propagationRejected, trace, Diagnostic.ERROR, PROPAGATION_ERROR);
			case ModelFacadeResult.ConcretizationRejected propagationRejected ->
					convertIssues(propagationRejected, trace, Diagnostic.INFO, CONCRETIZATION_ERROR);
		};
	}

	public InvalidProblemException wrapPropagationRejectedException(PropagationRejectedException e,
																	ProblemTrace trace) {
		var resourceUri = trace.getProblem().eResource().getURI();
		var issues = convertIssues(e.getReason(), e.getMessage(), trace, Diagnostic.ERROR, PROPAGATION_ERROR);
		return new InvalidProblemException(resourceUri, issues, null);
	}

	private List<Issue> convertIssues(ModelFacadeResult.Rejected rejectedResult, ProblemTrace trace, int severity,
									  String issueCode) {
		return convertIssues(rejectedResult.reason(), rejectedResult.formatMessage(), trace, severity, issueCode);
	}

	private List<Issue> convertIssues(Object reason, String message, ProblemTrace trace, int severity,
									  String issueCode) {
		var sourceElements = getSourceElements(reason, trace);
		return convertIssues(sourceElements, message, severity, issueCode);
	}

	private List<? extends EObject> getSourceElements(Object reason, ProblemTrace trace) {
		return switch (reason) {
			case MultiObjectTranslator ignored -> getScopeDeclarations(trace.getProblem());
			case ScopePropagator ignored -> getScopeDeclarations(trace.getProblem());
			case Rule rule -> getRuleDefinitions(rule, trace);
			default -> List.of();
		};
	}

	private List<Statement> getScopeDeclarations(Problem problem) {
		return problem.getStatements().stream()
				.filter(ScopeDeclaration.class::isInstance)
				.toList();
	}

	private List<RuleDefinition> getRuleDefinitions(Rule rule, ProblemTrace trace) {
		var ruleDefinition = trace.getInverseRuleDefinitionTrace().get(rule);
		if (ruleDefinition == null) {
			return List.of();
		}
		return List.of(ruleDefinition);
	}

	private List<Issue> convertIssues(List<? extends EObject> sourceElements, String message, int severity,
									  String issueCode) {
		if (sourceElements.isEmpty()) {
			var issue = new Issue.IssueImpl();
			issue.setMessage(message);
			issue.setSeverity(translateSeverity(severity));
			issue.setCode(issueCode);
			return List.of(issue);
		} else {
			return sourceElements.stream()
					.<Issue>mapMulti((sourceElement, consumer) -> {
						var diagnostic = new FeatureBasedDiagnostic(severity, message, sourceElement, null,
								0, CheckType.EXPENSIVE, issueCode);
						diagnosticConverter.convertValidatorDiagnostic(diagnostic, consumer::accept);
					})
					.toList();
		}
	}

	private static Severity translateSeverity(int severity) {
		return switch (severity) {
			case Diagnostic.INFO -> Severity.INFO;
			case Diagnostic.WARNING -> Severity.WARNING;
			case Diagnostic.ERROR -> Severity.ERROR;
			default -> throw new IllegalArgumentException("Unknown severity: " + severity);
		};
	}
}
