package tools.refinery.store.query.atom;

public enum SimplePolarity implements CallPolarity {
	POSITIVE(true, false),
	NEGATIVE(false, false),
	TRANSITIVE(true, true);

	private final boolean positive;

	private final boolean transitive;

	SimplePolarity(boolean positive, boolean transitive) {
		this.positive = positive;
		this.transitive = transitive;
	}

	@Override
	public boolean isPositive() {
		return positive;
	}

	@Override
	public boolean isTransitive() {
		return transitive;
	}
}
