/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;
import tools.refinery.store.reasoning.representation.PartialFunction;

import java.util.List;

class WrappedConstantFunctionAction<A extends AbstractValue<A, C>, C> extends WrappedFunctionAction<A, C> {
	private final A value;

	public WrappedConstantFunctionAction(
			RuleCompiler ruleCompiler, PreparedRule preparedRule, PartialFunction<A, C> partialFunction,
			List<AssertionArgument> problemArguments, A value) {
		super(ruleCompiler, preparedRule, partialFunction, problemArguments);
		this.value = value;
	}

	@Override
	public Term<A> getValueTerm(ConcretenessSpecification concreteness) {
		return new ConstantTerm<>(getPartialFunction().abstractDomain().abstractType(), value);
	}

	@Override
	protected ActionLiteral getActionLiteral(Concreteness concreteness, List<NodeVariable> arguments) {
		return PartialActionLiterals.merge(getPartialFunction(), value, arguments);
	}
}
