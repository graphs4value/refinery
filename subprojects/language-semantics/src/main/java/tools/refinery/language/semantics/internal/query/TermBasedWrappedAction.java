/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.literal.Literals;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.abstractdomain.AbstractDomainTerms;
import tools.refinery.logic.term.bool.BoolTerms;
import tools.refinery.logic.term.truthvalue.TruthValueTerms;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;
import tools.refinery.store.reasoning.representation.PartialSymbol;

import java.util.List;

abstract class TermBasedWrappedAction<A extends AbstractValue<A, C>, C> extends WrappedAction {
	private final PartialSymbol<A, C> partialSymbol;

	protected TermBasedWrappedAction(RuleCompiler ruleCompiler, PreparedRule preparedRule,
									 PartialSymbol<A, C> partialSymbol, List<AssertionArgument> problemArguments) {
		super(ruleCompiler, preparedRule, problemArguments);
		this.partialSymbol = partialSymbol;
	}

	public PartialSymbol<A, C> getPartialSymbol() {
		return partialSymbol;
	}

	@Override
	public boolean toLiterals(boolean positive, ConcreteModality concreteModality, List<Literal> literals) {
		var effectiveModality = positive ? concreteModality : concreteModality.negate();
		var concreteness = effectiveModality.concreteness();
		var valueTerm = getValueTerm(concreteness);
		if (valueTerm == null) {
			return true;
		}
		var arguments = List.of(collectArguments(effectiveModality, literals));
		var abstractDomain = partialSymbol.abstractDomain();
		var currentTerm = getCurrentTerm(concreteness, arguments);
		var expr = switch (effectiveModality.modality()) {
			case MAY -> TruthValueTerms.may(AbstractDomainTerms.eq(abstractDomain, currentTerm, valueTerm));
			case MUST -> AbstractDomainTerms.subset(abstractDomain, currentTerm, valueTerm);
			case UNSPECIFIED -> throw new IllegalArgumentException("Modality must be specified");
		};
		if (!positive) {
			expr = BoolTerms.not(expr);
		}
		literals.add(Literals.check(expr));
		return false;
	}

	protected abstract @NotNull Term<A> getCurrentTerm(ConcretenessSpecification concreteness,
                                                       List<NodeVariable> arguments);

	protected abstract @Nullable Term<A> getValueTerm(ConcretenessSpecification concreteness);
}
