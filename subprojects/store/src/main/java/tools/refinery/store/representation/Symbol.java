package tools.refinery.store.representation;

public record Symbol<T>(String name, int arity, Class<T> valueType, T defaultValue) implements AnySymbol {
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
