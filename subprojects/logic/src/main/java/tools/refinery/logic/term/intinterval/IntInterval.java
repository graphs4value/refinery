package tools.refinery.logic.term.intinterval;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.AbstractValue;

public record IntInterval(@NotNull Bound lowerBound, @NotNull Bound upperBound)
		implements AbstractValue<IntInterval, Integer> {
	public static final IntInterval UNKNOWN = new IntInterval(Bound.Infinite.NEGATIVE_INFINITY,
			Bound.Infinite.POSITIVE_INFINITY);
	public static final IntInterval ERROR = new IntInterval(Bound.Infinite.POSITIVE_INFINITY,
			Bound.Infinite.NEGATIVE_INFINITY);

	@Override
	public @Nullable Integer getConcrete() {
		if (lowerBound.equals(upperBound) && lowerBound instanceof Bound.Finite(int value)) {
			return value;
		}
		return null;
	}

	@Override
	public boolean isConcrete() {
		return lowerBound.equals(upperBound) && lowerBound.isFinite();
	}

	@Override
	public @Nullable Integer getArbitrary() {
		if (isError()) {
			return null;
		}
		if (lowerBound instanceof Bound.Finite(int value)) {
			return value;
		}
		if (upperBound instanceof Bound.Finite(int value)) {
			return value;
		}
		return 0;
	}

	@Override
	public boolean isError() {
		if (lowerBound.lessThanOrEquals(upperBound)) {
			return lowerBound == Bound.Infinite.POSITIVE_INFINITY ||
					upperBound == Bound.Infinite.NEGATIVE_INFINITY;
		}
		return true;
	}

	@Override
	public IntInterval join(IntInterval other) {
		return new IntInterval(lowerBound.min(other.lowerBound),upperBound.max(other.upperBound));
	}

	@Override
	public IntInterval meet(IntInterval other) {
		return new IntInterval(lowerBound.max(other.lowerBound),upperBound.min(other.upperBound));
	}

	public static IntInterval of(int value) {
		var bound = Bound.of(value);
		return new IntInterval(bound, bound);
	}

	public static IntInterval of(int value1, int value2) {
		var bound1 = Bound.of(value1);
		var bound2 = Bound.of(value2);
		return new IntInterval(bound1, bound2);
	}

	public static IntInterval of(int value, Bound bound) {
		var valueBound = Bound.of(value);
		return new IntInterval(valueBound, bound);
	}

	public static IntInterval of(Bound bound, int value) {
		var valueBound = Bound.of(value);
		return new IntInterval(bound, valueBound);
	}

	public static IntInterval of(Bound bound1, Bound bound2) {
		return new IntInterval(bound1, bound2);
	}

	@Override
	public String toString() {
		return "(%s, %s)".formatted(lowerBound(), upperBound());
	}

	public IntInterval add(IntInterval other){
		return new IntInterval(lowerBound().add(other.lowerBound()),upperBound().add(other.upperBound()));
	}

	public IntInterval sub(IntInterval other){
		return new IntInterval(lowerBound().sub(other.upperBound()),
				upperBound().sub(other.lowerBound()));
	}

	public IntInterval mul(IntInterval other){
		return new IntInterval(lowerBound().mul(other.lowerBound())
				.min(lowerBound().mul(other.upperBound()))
				.min(upperBound().mul(other.lowerBound()))
				.min(upperBound().mul(other.upperBound())),
				lowerBound().mul(other.lowerBound())
				.max(lowerBound().mul(other.upperBound()))
				.max(upperBound().mul(other.lowerBound()))
				.max(upperBound().mul(other.upperBound())));
	}
}
