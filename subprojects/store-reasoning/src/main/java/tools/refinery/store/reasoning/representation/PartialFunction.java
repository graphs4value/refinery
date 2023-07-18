/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.representation;

import tools.refinery.store.representation.AbstractDomain;

public record PartialFunction<A, C>(String name, int arity, AbstractDomain<A, C> abstractDomain)
		implements AnyPartialFunction, PartialSymbol<A, C> {
	@Override
	public A defaultValue() {
		return null;
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
