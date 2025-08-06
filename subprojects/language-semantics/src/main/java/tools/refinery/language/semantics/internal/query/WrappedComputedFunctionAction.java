/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;
import tools.refinery.store.reasoning.representation.PartialFunction;

import java.util.LinkedHashSet;
import java.util.List;

class WrappedComputedFunctionAction<A extends AbstractValue<A, C>, C> extends WrappedFunctionAction<A, C> {
	private final ComputedHelper<A, C> helper;

	public WrappedComputedFunctionAction(
			RuleCompiler ruleCompiler, PreparedRule preparedRule, PartialFunction<A, C> partialFunction,
			List<AssertionArgument> problemArguments, List<Literal> literals, Term<A> valueTerm) {
		super(ruleCompiler, preparedRule, partialFunction, problemArguments);
		helper = new ComputedHelper<>(preparedRule, partialFunction, literals, valueTerm);
	}

	@Override
	protected void findNodeVariables(LinkedHashSet<NodeVariable> parameterSet) {
		super.findNodeVariables(parameterSet);
		helper.findNodeVariables(parameterSet);
	}

	@Override
	protected Term<A> getValueTerm(ConcretenessSpecification concreteness) {
		return helper.toValueTerm(concreteness);
	}

	@Override
	public void setPrecondition(RelationalQuery precondition) {
		helper.setPrecondition(precondition);
	}

	@Override
	protected ActionLiteral getActionLiteral(Concreteness concreteness, List<NodeVariable> arguments) {
		return helper.toActionLiteral(concreteness, arguments);
	}
}
