/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import tools.refinery.language.expressions.ExprToTerm;
import tools.refinery.language.model.problem.Atom;
import tools.refinery.language.model.problem.Expr;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.logic.literal.Literal;
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
		if (expr instanceof Atom atom) {
			var argumentList = queryCompiler.toArgumentList(atom, atom.getArguments(), localScope, literals);
			var partialFunction = problemTrace.getPartialFunction(atom.getRelation());
			return Optional.of(partialFunction.call(ConcretenessSpecification.UNSPECIFIED, argumentList.arguments()));
		} else {
			return super.toTerm(expr);
		}
	}
}
