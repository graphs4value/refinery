package tools.refinery.store.representation.cardinality;

import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;

public record NonEmptyCardinalityInterval(int lowerBound, UpperCardinality upperBound) implements CardinalityInterval {
	public NonEmptyCardinalityInterval {
		if (lowerBound < 0) {
			throw new IllegalArgumentException("lowerBound must not be negative");
		}
		if (upperBound.compareToInt(lowerBound) < 0) {
			throw new IllegalArgumentException("lowerBound must not be larger than upperBound");
		}
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public CardinalityInterval min(CardinalityInterval other) {
		return lift(other, Math::min, UpperCardinality::min);
	}

	@Override
	public CardinalityInterval max(CardinalityInterval other) {
		return lift(other, Math::max, UpperCardinality::max);
	}

	@Override
	public CardinalityInterval add(CardinalityInterval other) {
		return lift(other, Integer::sum, UpperCardinality::add);
	}

	@Override
	public CardinalityInterval multiply(CardinalityInterval other) {
		return lift(other, (a, b) -> a * b, UpperCardinality::multiply);
	}

	@Override
	public CardinalityInterval meet(CardinalityInterval other) {
		return lift(other, Math::max, UpperCardinality::min);
	}

	@Override
	public CardinalityInterval join(CardinalityInterval other) {
		return lift(other, Math::min, UpperCardinality::max, this);
	}

	private CardinalityInterval lift(CardinalityInterval other, IntBinaryOperator lowerOperator,
									 BinaryOperator<UpperCardinality> upperOperator,
									 CardinalityInterval whenEmpty) {
		if (other instanceof NonEmptyCardinalityInterval nonEmptyOther) {
			return CardinalityIntervals.between(lowerOperator.applyAsInt(lowerBound, nonEmptyOther.lowerBound),
					upperOperator.apply(upperBound, nonEmptyOther.upperBound));
		}
		if (other instanceof EmptyCardinalityInterval) {
			return whenEmpty;
		}
		throw new IllegalArgumentException("Unknown CardinalityInterval: " + other);
	}

	private CardinalityInterval lift(CardinalityInterval other, IntBinaryOperator lowerOperator,
									 BinaryOperator<UpperCardinality> upperOperator) {
		return lift(other, lowerOperator, upperOperator, CardinalityIntervals.ERROR);
	}

	@Override
	public String toString() {
		var closeBracket = upperBound instanceof UnboundedUpperCardinality ? ")" : "]";
		return "[%d..%s%s".formatted(lowerBound, upperBound, closeBracket);
	}
}
