/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.InvalidQueryException;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.rewriter.TermRewriter;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.*;
import tools.refinery.logic.valuation.Valuation;
import tools.refinery.store.reasoning.representation.PartialFunction;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PartialFunctionCallTerm<A extends AbstractValue<A, C>, C> extends AbstractTerm<A>
		implements PartialTerm<A> {
	private final ConcretenessSpecification concreteness;
	private final PartialFunction<A, C> partialFunction;
	private final List<NodeVariable> arguments;

	public PartialFunctionCallTerm(ConcretenessSpecification concreteness, PartialFunction<A, C> partialFunction,
								   List<NodeVariable> arguments) {
		super(partialFunction.abstractDomain().abstractType());
		this.concreteness = concreteness;
		this.partialFunction = partialFunction;
		this.arguments = arguments;
		if (arguments.size() != partialFunction.arity()) {
			throw new InvalidQueryException("Expected %d arguments for partial function %s, got %d arguments instead"
					.formatted(partialFunction.arity(), partialFunction, arguments.size()));
		}
	}

	public ConcretenessSpecification getConcreteness() {
		return concreteness;
	}

	public PartialFunction<A, C> getPartialFunction() {
		return partialFunction;
	}

	public List<NodeVariable> getArguments() {
		return arguments;
	}

	@Override
	public A evaluate(Valuation valuation) {
		throw new IllegalStateException("Partial function call %s cannot be evaluated directly.".formatted(this));
	}

	@Override
	public Term<A> rewriteSubTerms(TermRewriter termRewriter) {
		return this;
	}

	@Override
	public Term<A> substitute(Substitution substitution) {
		var substitutedArguments = arguments.stream()
				.map(substitution::getTypeSafeSubstitute)
				.toList();
		return new PartialFunctionCallTerm<>(concreteness, partialFunction, substitutedArguments);
	}

	@Override
	public PartialFunctionCallTerm<A, C> orElseConcreteness(Concreteness fallback) {
		var newConcreteness = concreteness.orElse(fallback.toSpecification());
		if (concreteness == newConcreteness) {
			return this;
		}
		return new PartialFunctionCallTerm<>(newConcreteness, partialFunction, arguments);
	}

	@Override
	public Set<Variable> getVariables() {
		return Set.copyOf(arguments);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherCallTerm = (PartialFunctionCallTerm<?, ?>) other;
		if (concreteness != otherCallTerm.concreteness ||
				!partialFunction.equals(otherCallTerm.partialFunction)) {
			return false;
		}
		int arity = arguments.size();
		for (int i = 0; i < arity; i++) {
			if (!helper.variableEqual(arguments.get(i), otherCallTerm.arguments.get(i))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		int value = Objects.hash(super.hashCodeWithSubstitution(helper), concreteness, partialFunction);
		for (var argument : arguments) {
			value = value * 31 + helper.getVariableHashCode(argument);
		}
		return value;
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		if (concreteness != ConcretenessSpecification.UNSPECIFIED) {
			builder.append(concreteness).append(" ");
		}
		return builder.append(partialFunction.name())
				.append("(")
				.append(arguments.stream().map(Variable::getName).collect(Collectors.joining(", ")))
				.append(")")
				.toString();
	}
}
