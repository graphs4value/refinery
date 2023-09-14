/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.representation;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.term.Parameter;
import tools.refinery.store.representation.AbstractDomain;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.TruthValueDomain;

import java.util.Arrays;
import java.util.List;

public record PartialRelation(String name, int arity) implements PartialSymbol<TruthValue, Boolean>, Constraint {
	@Override
	public AbstractDomain<TruthValue, Boolean> abstractDomain() {
		return TruthValueDomain.INSTANCE;
	}

	@Override
	public TruthValue defaultValue() {
		return TruthValue.FALSE;
	}

	@Override
	public List<Parameter> getParameters() {
		var parameters = new Parameter[arity];
		Arrays.fill(parameters, Parameter.NODE_OUT);
		return List.of(parameters);
	}

	@Override
	public String toReferenceString() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		// Compare by identity to make hash table lookups more efficient.
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		return "%s/%d".formatted(name, arity);
	}
}
