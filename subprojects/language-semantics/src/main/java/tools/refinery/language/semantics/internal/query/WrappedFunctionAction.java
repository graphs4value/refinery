/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import org.jetbrains.annotations.NotNull;
import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;
import tools.refinery.store.reasoning.representation.PartialFunction;

import java.util.List;

abstract class WrappedFunctionAction<A extends AbstractValue<A, C>, C> extends TermBasedWrappedAction<A, C> {
	protected WrappedFunctionAction(
			RuleCompiler ruleCompiler, PreparedRule preparedRule, PartialFunction<A, C> partialFunction,
			List<AssertionArgument> problemArguments) {
		super(ruleCompiler, preparedRule, partialFunction, problemArguments);
	}

	public PartialFunction<A, C> getPartialFunction() {
		return (PartialFunction<A, C>) getPartialSymbol();
	}

	@Override
	protected @NotNull Term<A> getCurrentTerm(ConcretenessSpecification concreteness, List<NodeVariable> arguments) {
		return getPartialFunction().call(concreteness, arguments);
	}
}
