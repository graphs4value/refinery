/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialFunction;

import java.util.List;
import java.util.Map;

class WrappedConstantFunctionAction<A extends AbstractValue<A, C>, C> extends WrappedFunctionAction<A, C> {
	private final A value;

	public WrappedConstantFunctionAction(
			RuleCompiler ruleCompiler, PreparedRule preparedRule, PartialFunction<A, C> partialFunction,
			List<AssertionArgument> problemArguments, A value) {
		super(ruleCompiler, preparedRule, partialFunction, problemArguments);
		this.value = value;
	}

	@Override
	public boolean toLiterals(boolean positive, ConcreteModality concreteModality, List<Literal> literals) {
		var valueConstant = new ConstantTerm<>(getPartialFunction().abstractDomain().abstractType(), value);
		toLiterals(valueConstant, positive, concreteModality, literals);
		return false;
	}

	@Override
	public void toActionLiterals(Concreteness concreteness, List<ActionLiteral> actionLiterals,
								 Map<tools.refinery.language.model.problem.Variable, NodeVariable> localScope) {
		var arguments = collectArguments(localScope, actionLiterals);
		actionLiterals.add(PartialActionLiterals.merge(getPartialFunction(), value, arguments));
	}
}
