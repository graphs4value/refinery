/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import tools.refinery.language.expressions.ExprToTerm;
import tools.refinery.language.model.problem.Atom;
import tools.refinery.language.model.problem.Expr;
import tools.refinery.language.model.problem.VariableOrNodeExpr;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.AnyDataVariable;
import tools.refinery.logic.term.AnyTerm;
import tools.refinery.logic.term.Variable;
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
			case Atom atom -> {
				var argumentList = queryCompiler.toArgumentList(atom, atom.getArguments(), localScope, literals);
				var partialFunction = problemTrace.getPartialFunction(atom.getRelation());
				yield Optional.of(partialFunction.call(ConcretenessSpecification.UNSPECIFIED,
						argumentList.arguments()));
			}
			case VariableOrNodeExpr variableOrNodeExpr -> {
				if (variableOrNodeExpr.getVariableOrNode() instanceof
						tools.refinery.language.model.problem.Variable problemVariable &&
						localScope.get(problemVariable) instanceof AnyDataVariable variable) {
					yield Optional.of(variable);
				} else {
					yield Optional.empty();
				}
			}
			case null, default -> super.toTerm(expr);
		};
	}
}
