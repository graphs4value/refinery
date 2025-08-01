/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.literal.Literals;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.abstractdomain.AbstractDomainTerms;
import tools.refinery.logic.term.bool.BoolTerms;
import tools.refinery.logic.term.truthvalue.TruthValueTerms;
import tools.refinery.store.reasoning.representation.PartialFunction;

import java.util.List;

abstract class WrappedFunctionAction<A extends AbstractValue<A, C>, C> extends WrappedAction {
	private final PartialFunction<A, C> partialFunction;

	protected WrappedFunctionAction(
			RuleCompiler ruleCompiler, PreparedRule preparedRule, PartialFunction<A, C> partialFunction,
			List<AssertionArgument> problemArguments) {
		super(ruleCompiler, preparedRule, problemArguments);
		this.partialFunction = partialFunction;
	}

	public PartialFunction<A, C> getPartialFunction() {
		return partialFunction;
	}

	protected void toLiterals(Term<A> valueTerm, boolean positive, ConcreteModality concreteModality,
							  List<Literal> literals) {
		var effectiveModality = positive ? concreteModality : concreteModality.negate();
		var arguments = collectArguments(effectiveModality, literals);
		var abstractDomain = partialFunction.abstractDomain();
		var callResult = Variable.of(abstractDomain.abstractType());
		literals.add(callResult.assign(partialFunction.call(effectiveModality.concreteness(), List.of(arguments))));
		var expr = switch (effectiveModality.modality()) {
			case MAY -> TruthValueTerms.may(AbstractDomainTerms.eq(abstractDomain, callResult, valueTerm));
			case MUST -> AbstractDomainTerms.subset(abstractDomain, callResult, valueTerm);
			case UNSPECIFIED -> throw new IllegalArgumentException("Modality must be specified");
		};
		if (!positive) {
			expr = BoolTerms.not(expr);
		}
		literals.add(Literals.check(expr));
	}
}
