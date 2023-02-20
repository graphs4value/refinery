package tools.refinery.store.partial.representation;

import tools.refinery.store.representation.AbstractDomain;

public record PartialFunction<A, C>(String name, int arity, AbstractDomain<A, C> abstractDomain)
		implements AnyPartialFunction, PartialSymbol<A, C> {
	@Override
	public A defaultValue() {
		return null;
	}

	@Override
	public C defaultConcreteValue() {
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
