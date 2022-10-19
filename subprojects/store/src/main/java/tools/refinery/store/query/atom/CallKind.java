package tools.refinery.store.query.atom;

public enum CallKind {
	POSITIVE(true, false),
	NEGATIVE(false, false),
	TRANSITIVE(true, true);

	private final boolean positive;

	private final boolean transitive;

	CallKind(boolean positive, boolean transitive) {
		this.positive = positive;
		this.transitive = transitive;
	}

	public boolean isPositive() {
		return positive;
	}

	public boolean isTransitive() {
		return transitive;
	}
}
