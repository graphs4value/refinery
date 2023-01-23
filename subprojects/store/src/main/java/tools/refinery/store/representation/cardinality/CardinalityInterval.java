package tools.refinery.store.representation.cardinality;

public sealed interface CardinalityInterval permits NonEmptyCardinalityInterval, EmptyCardinalityInterval {
	int lowerBound();

	UpperCardinality upperBound();

	boolean isEmpty();

	CardinalityInterval min(CardinalityInterval other);

	CardinalityInterval max(CardinalityInterval other);

	CardinalityInterval add(CardinalityInterval other);

	CardinalityInterval multiply(CardinalityInterval other);

	CardinalityInterval meet(CardinalityInterval other);

	CardinalityInterval join(CardinalityInterval other);
}
