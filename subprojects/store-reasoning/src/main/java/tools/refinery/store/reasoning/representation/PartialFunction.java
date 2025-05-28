/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.representation;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;
import tools.refinery.store.reasoning.literal.PartialFunctionCallTerm;

import java.util.List;

public record PartialFunction<A extends AbstractValue<A, C>, C>(
		String name, int arity, AbstractDomain<A, C> abstractDomain) implements AnyPartialFunction,
		PartialSymbol<A, C> {
	@Override
	public A defaultValue() {
		return abstractDomain().error();
	}

	@Override
	public Term<A> call(NodeVariable... arguments) {
		return call(ConcretenessSpecification.UNSPECIFIED, List.of(arguments));
	}

	@Override
	public Term<A> call(Concreteness concreteness, NodeVariable... arguments) {
		return call(concreteness.toSpecification(), List.of(arguments));
	}

	@Override
	public Term<A> call(ConcretenessSpecification concreteness, List<NodeVariable> arguments) {
		return new PartialFunctionCallTerm<>(concreteness, this, arguments);
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		// Compare by identity to make hash table look-ups more efficient.
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		return "%s/%d".formatted(name, arity);
	}
}
