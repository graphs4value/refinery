package tools.refinery.store.representation.cardinality;

public final class EmptyCardinalityInterval implements CardinalityInterval {
	static final EmptyCardinalityInterval INSTANCE = new EmptyCardinalityInterval();

	private EmptyCardinalityInterval() {
		// Singleton constructor.
	}

	@Override
	public int lowerBound() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public UpperCardinality upperBound() {
		return UpperCardinalities.ZERO;
	}

	@Override
	public CardinalityInterval min(CardinalityInterval other) {
		return this;
	}

	@Override
	public CardinalityInterval max(CardinalityInterval other) {
		return this;
	}

	@Override
	public CardinalityInterval add(CardinalityInterval other) {
		return this;
	}

	@Override
	public CardinalityInterval multiply(CardinalityInterval other) {
		return this;
	}

	@Override
	public CardinalityInterval meet(CardinalityInterval other) {
		return this;
	}

	@Override
	public CardinalityInterval join(CardinalityInterval other) {
		return other;
	}

	@Override
	public String toString() {
		return "error";
	}
}
