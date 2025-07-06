/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import org.jetbrains.annotations.NotNull;
import tools.refinery.language.expressions.ExprToTerm;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.AnyDataVariable;
import tools.refinery.logic.term.AnyTerm;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueTerms;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class QueryBasedExprToTerm extends ExprToTerm {
	private QueryCompiler queryCompiler;

	private ProblemTrace problemTrace;

	private Map<tools.refinery.language.model.problem.Variable, ? extends Variable> localScope;

	private List<Literal> literals;

	public void setLiterals(List<Literal> literals) {
		this.literals = literals;
	}

	public void setLocalScope(Map<tools.refinery.language.model.problem.Variable, ? extends Variable> localScope) {
		this.localScope = localScope;
	}

	public void setProblemTrace(ProblemTrace problemTrace) {
		this.problemTrace = problemTrace;
	}

	public void setQueryCompiler(QueryCompiler queryCompiler) {
		this.queryCompiler = queryCompiler;
	}

	public Optional<AnyTerm> toTerm(Expr expr) {
		return switch (expr) {
			case Atom atom -> createPartialFunctionCall(atom);
			case VariableOrNodeExpr variableOrNodeExpr -> createVariableReference(variableOrNodeExpr);
			case ModalExpr modalExpr -> createModalOperator(modalExpr);
			case null, default -> super.toTerm(expr);
		};
	}


	private Optional<AnyTerm> createPartialFunctionCall(Atom atom) {
		return createPartialFunctionCall(atom, ConcretenessSpecification.UNSPECIFIED);
	}

	private Optional<AnyTerm> createPartialFunctionCall(Atom atom, ConcretenessSpecification concreteness) {
		var argumentList = queryCompiler.toArgumentList(atom, atom.getArguments(), localScope, literals);
		var partialFunction = problemTrace.getPartialFunction(atom.getRelation());
		return Optional.of(partialFunction.call(concreteness, argumentList.arguments()));
	}

	private @NotNull Optional<AnyTerm> createVariableReference(VariableOrNodeExpr variableOrNodeExpr) {
		if (variableOrNodeExpr.getVariableOrNode() instanceof
				tools.refinery.language.model.problem.Variable problemVariable &&
				localScope.get(problemVariable) instanceof AnyDataVariable variable) {
			return Optional.of(variable);
		} else {
			return Optional.empty();
		}
	}

	private Optional<AnyTerm> createModalOperator(ModalExpr expr) {
		var concreteness = expr.getConcreteness();
		if (concreteness == Concreteness.UNSPECIFIED) {
			return super.toTerm(expr);
		}
		var concretenessSpecification = switch (concreteness) {
			case PARTIAL -> ConcretenessSpecification.PARTIAL;
			case CANDIDATE -> ConcretenessSpecification.CANDIDATE;
			default -> throw new IllegalArgumentException("Unsupported concreteness: " + concreteness);
		};
		Optional<AnyTerm> result;
		var body = expr.getBody();
		if (body instanceof Atom atom) {
			result = createPartialFunctionCall(atom, concretenessSpecification);
		} else if (body instanceof NegationExpr negationExpr && negationExpr.getBody() instanceof Atom atom) {
			result = createPartialFunctionCall(atom, concretenessSpecification)
					.map(callTerm -> TruthValueTerms.not(callTerm.asType(TruthValue.class)));
		} else {
			// Concreteness specifications must be applied to partial function calls directly.
			return Optional.empty();
		}
		return result.map(callTerm -> wrapModality(callTerm, expr.getModality()));
	}
}
