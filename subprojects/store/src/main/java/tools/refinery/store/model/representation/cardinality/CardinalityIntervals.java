package tools.refinery.store.model.representation.cardinality;

public final class CardinalityIntervals {
	public static final CardinalityInterval NONE = exactly(0);

	public static final CardinalityInterval ONE = exactly(1);

	public static final CardinalityInterval LONE = atMost(1);

	public static final CardinalityInterval SET = atLeast(0);

	public static final CardinalityInterval SOME = atLeast(1);

	public static final CardinalityInterval ERROR = EmptyCardinalityInterval.INSTANCE;

	private CardinalityIntervals() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static CardinalityInterval between(int lowerBound, UpperCardinality upperBound) {
		if (upperBound.compareToInt(lowerBound) < 0) {
			return ERROR;
		}
		return new NonEmptyCardinalityInterval(lowerBound, upperBound);
	}

	public static CardinalityInterval between(int lowerBound, int upperBound) {
		return between(lowerBound, UpperCardinalities.valueOf(upperBound));
	}

	public static CardinalityInterval atMost(UpperCardinality upperBound) {
		return new NonEmptyCardinalityInterval(0, upperBound);
	}

	public static CardinalityInterval atMost(int upperBound) {
		return atMost(UpperCardinalities.valueOf(upperBound));
	}

	public static CardinalityInterval atLeast(int lowerBound) {
		return new NonEmptyCardinalityInterval(lowerBound, UpperCardinalities.UNBOUNDED);
	}

	public static CardinalityInterval exactly(int lowerBound) {
		return new NonEmptyCardinalityInterval(lowerBound, UpperCardinalities.valueOf(lowerBound));
	}
}
