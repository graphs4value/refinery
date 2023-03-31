package tools.refinery.store.query.literal;

public enum CallPolarity {
	POSITIVE(true, false),
	NEGATIVE(false, false),
	TRANSITIVE(true, true);

	private final boolean positive;

	private final boolean transitive;

	CallPolarity(boolean positive, boolean transitive) {
		this.positive = positive;
		this.transitive = transitive;
	}

	public boolean isPositive() {
		return positive;
	}

	public boolean isTransitive() {
		return transitive;
	}

	public CallPolarity negate() {
		return switch (this) {
			case POSITIVE -> NEGATIVE;
			case NEGATIVE -> POSITIVE;
			case TRANSITIVE -> throw new IllegalArgumentException("Transitive polarity cannot be negated");
		};
	}
}
