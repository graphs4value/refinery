/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.intinterval;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.term.ComparableAbstractValue;
import tools.refinery.logic.term.operators.*;
import tools.refinery.logic.term.truthvalue.TruthValue;

public record IntInterval(@NotNull IntBound lowerBound, @NotNull IntBound upperBound)
		implements ComparableAbstractValue<IntInterval, Integer>, Comparable<IntInterval>, Plus<IntInterval>,
		Minus<IntInterval>, Add<IntInterval>, Sub<IntInterval>, Mul<IntInterval>, Div<IntInterval> {
	public static final IntInterval ZERO = new IntInterval(IntBound.Finite.ZERO, IntBound.Finite.ZERO);
	public static final IntInterval ONE = new IntInterval(IntBound.Finite.ONE, IntBound.Finite.ONE);
	public static final IntInterval UNKNOWN = new IntInterval(IntBound.Infinite.NEGATIVE_INFINITY,
			IntBound.Infinite.POSITIVE_INFINITY);
	public static final IntInterval ERROR = new IntInterval(IntBound.Infinite.POSITIVE_INFINITY,
			IntBound.Infinite.NEGATIVE_INFINITY);
	public static final IntInterval NEGATIVE_INFINITY = new IntInterval(IntBound.Infinite.NEGATIVE_INFINITY,
			IntBound.Infinite.NEGATIVE_INFINITY);
	public static final IntInterval POSITIVE_INFINITY = new IntInterval(IntBound.Infinite.POSITIVE_INFINITY,
			IntBound.Infinite.POSITIVE_INFINITY);

	@Override
	public @Nullable Integer getConcrete() {
		if (lowerBound.equals(upperBound) && lowerBound instanceof IntBound.Finite(int value)) {
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
		if (lowerBound instanceof IntBound.Finite(int value)) {
			return value;
		}
		if (upperBound instanceof IntBound.Finite(int value)) {
			return value;
		}
		return 0;
	}

	@Override
	public boolean isError() {
		if (lowerBound.lessThanOrEquals(upperBound)) {
			return lowerBound == IntBound.Infinite.POSITIVE_INFINITY ||
					upperBound == IntBound.Infinite.NEGATIVE_INFINITY;
		}
		return true;
	}

	@Override
	public IntInterval join(IntInterval other) {
		return new IntInterval(lowerBound.min(other.lowerBound), upperBound.max(other.upperBound));
	}

	@Override
	public IntInterval meet(IntInterval other) {
		return new IntInterval(lowerBound.max(other.lowerBound), upperBound.min(other.upperBound));
	}

	public static IntInterval of(int value) {
		var bound = IntBound.of(value);
		return new IntInterval(bound, bound);
	}

	public static IntInterval of(int value1, int value2) {
		var bound1 = IntBound.of(value1);
		var bound2 = IntBound.of(value2);
		return new IntInterval(bound1, bound2);
	}

	public static IntInterval of(int value, IntBound bound) {
		var valueBound = IntBound.of(value);
		return new IntInterval(valueBound, bound);
	}

	public static IntInterval of(IntBound bound, int value) {
		var valueBound = IntBound.of(value);
		return new IntInterval(bound, valueBound);
	}

	public static IntInterval of(IntBound bound1, IntBound bound2) {
		return new IntInterval(bound1, bound2);
	}

	@Override
	public @NotNull String toString() {
		if (lowerBound.equals(upperBound)) {
			return lowerBound().toString();
		}
		if (IntBound.Infinite.NEGATIVE_INFINITY.equals(lowerBound) &&
				IntBound.Infinite.POSITIVE_INFINITY.equals(upperBound)) {
			return "unknown";
		}
		if (IntBound.Infinite.POSITIVE_INFINITY.equals(lowerBound) &&
				IntBound.Infinite.NEGATIVE_INFINITY.equals(upperBound)) {
			return "error";
		}
		var builder = new StringBuilder();
		if (IntBound.Infinite.NEGATIVE_INFINITY.equals(lowerBound)) {
			builder.append("*");
		} else {
			builder.append(lowerBound);
		}
		builder.append("..");
		if (IntBound.Infinite.POSITIVE_INFINITY.equals(upperBound)) {
			builder.append("*");
		} else {
			builder.append(upperBound);
		}
		return builder.toString();
	}

	@Override
	public int compareTo(@NotNull IntInterval other) {
		int result = lowerBound.compareBound(other.lowerBound);
		return result == 0 ? upperBound.compareBound(other.upperBound) : result;
	}

	@Override
	public IntInterval plus() {
		return this;
	}

	@Override
	public IntInterval minus() {
		return of(upperBound().minus(), lowerBound().minus());
	}

	@Override
	public IntInterval add(IntInterval other) {
		return of(lowerBound().add(other.lowerBound(), IntBound.Infinite.POSITIVE_INFINITY),
				upperBound().add(other.upperBound(), IntBound.Infinite.NEGATIVE_INFINITY));
	}

	@Override
	public IntInterval sub(IntInterval other) {
		return of(lowerBound().sub(other.upperBound(), IntBound.Infinite.POSITIVE_INFINITY),
				upperBound().sub(other.lowerBound(), IntBound.Infinite.NEGATIVE_INFINITY));
	}

	@Override
	public IntInterval mul(IntInterval other) {
		// Kaucher, E. (1980). Interval Analysis in the Extended Interval Space IR. In: Fundamentals of Numerical
		// Computation (Computer-Oriented Numerical Analysis). Springer. https://doi.org/10.1007/978-3-7091-8577-3_3
		var otherLowerBound = other.lowerBound();
		var otherUpperBound = other.upperBound();
		return switch (positivity()) {
			case POSITIVE -> switch (other.positivity()) {
				case POSITIVE -> of(lowerBound.mul(otherLowerBound), upperBound.mul(otherUpperBound));
				case ZERO_PROPER -> of(upperBound.mul(otherLowerBound), upperBound.mul(otherUpperBound));
				case NEGATIVE -> of(upperBound.mul(otherLowerBound), lowerBound.mul(otherUpperBound));
				case ZERO_IMPROPER -> of(lowerBound.mul(otherLowerBound), lowerBound.mul(otherUpperBound));
			};
			case ZERO_PROPER -> switch (other.positivity()) {
				case POSITIVE -> of(lowerBound.mul(otherUpperBound), upperBound.mul(otherUpperBound));
				case ZERO_PROPER -> of(
						lowerBound.mul(otherUpperBound).min(upperBound.mul(otherLowerBound)),
						lowerBound.mul(otherLowerBound).max(upperBound.mul(otherUpperBound))
				);
				case NEGATIVE -> of(upperBound.mul(otherLowerBound), lowerBound.mul(otherLowerBound));
				case ZERO_IMPROPER -> IntInterval.ZERO;
			};
			case NEGATIVE -> switch (other.positivity()) {
				case POSITIVE -> of(lowerBound.mul(otherUpperBound), upperBound.mul(otherLowerBound));
				case ZERO_PROPER -> of(lowerBound.mul(otherUpperBound), lowerBound.mul(otherLowerBound));
				case NEGATIVE -> of(upperBound.mul(otherUpperBound), lowerBound.mul(otherLowerBound));
				case ZERO_IMPROPER -> of(upperBound.mul(otherUpperBound), upperBound.mul(otherLowerBound));
			};
			case ZERO_IMPROPER -> switch (other.positivity()) {
				case POSITIVE -> of(lowerBound.mul(otherLowerBound), upperBound.mul(otherLowerBound));
				case ZERO_PROPER -> IntInterval.ZERO;
				case NEGATIVE -> of(upperBound.mul(otherUpperBound), lowerBound.mul(otherUpperBound));
				case ZERO_IMPROPER -> of(
						lowerBound.mul(otherLowerBound).max(upperBound.mul(otherUpperBound)),
						lowerBound.mul(otherUpperBound).min(upperBound.mul(otherLowerBound))
				);
			};
		};
	}

	@Override
	public IntInterval div(IntInterval other) {
		var otherLowerBound = other.lowerBound();
		var otherUpperBound = other.upperBound();
		IntInterval negativeResult = null;
		if (otherLowerBound.signum() < 0) {
			var negativeDivisor = IntInterval.of(otherLowerBound, otherUpperBound.min(IntBound.Finite.NEGATIVE_ONE));
			negativeResult = divWithNegative(negativeDivisor);
		}
		if (otherUpperBound.signum() > 0) {
			var positiveDivisor = IntInterval.of(otherLowerBound.max(IntBound.Finite.ONE), otherUpperBound);
            var positiveResult = divWithPositive(positiveDivisor);
			return negativeResult == null ? positiveResult : positiveResult.join(negativeResult);
		}
		return negativeResult != null ? negativeResult : IntInterval.ERROR;
	}

	private IntInterval divWithNegative(IntInterval other) {
		var otherLowerBound = other.lowerBound();
		var otherUpperBound = other.upperBound();
		return switch (positivity()) {
			case POSITIVE -> of(upperBound.div(otherUpperBound), lowerBound.div(otherLowerBound));
			case ZERO_PROPER -> of(upperBound.div(otherUpperBound), lowerBound.div(otherUpperBound));
			case NEGATIVE -> of(upperBound.div(otherLowerBound), lowerBound.div(otherUpperBound));
			case ZERO_IMPROPER -> of(upperBound.div(otherLowerBound), lowerBound.div(otherLowerBound));
		};
	}

	private IntInterval divWithPositive(IntInterval other) {
		var otherLowerBound = other.lowerBound();
		var otherUpperBound = other.upperBound();
		return switch (positivity()) {
			case POSITIVE -> of(lowerBound.div(otherUpperBound), upperBound.div(otherLowerBound));
			case ZERO_PROPER -> of(lowerBound.div(otherLowerBound), upperBound.div(otherLowerBound));
			case NEGATIVE -> of(lowerBound.div(otherLowerBound), upperBound.div(otherUpperBound));
			case ZERO_IMPROPER -> of(lowerBound.div(otherUpperBound), upperBound.div(otherUpperBound));
		};
	}

	@Override
	public TruthValue checkEquals(IntInterval other) {
		return checkLessEq(other).and(other.checkLessEq(this));
	}

	@Override
	public TruthValue checkLess(IntInterval other) {
		return other.checkLessEq(this).not();
	}

	@Override
	public TruthValue checkLessEq(IntInterval other) {
		var may = lowerBound().lessThanOrEquals(other.upperBound());
		var must = upperBound().lessThanOrEquals(other.lowerBound());
		return TruthValue.of(may, must);
	}

	@Override
	public IntInterval upToIncluding(IntInterval other) {
		return IntInterval.of(lowerBound(), other.upperBound());
	}

	@Override
	public IntInterval min(IntInterval other) {
		return of(lowerBound().min(other.lowerBound()), upperBound().min(other.upperBound()));
	}

	@Override
	public IntInterval max(IntInterval other) {
		return of(lowerBound().max(other.lowerBound()), upperBound().max(other.upperBound()));
	}

	@Override
	public IntInterval abstractLowerBound() {
		return IntInterval.of(lowerBound, lowerBound);
	}

	@Override
	public IntInterval abstractUpperBound() {
		return IntInterval.of(upperBound, upperBound);
	}

	private Positivity positivity() {
		int lowerCompare = lowerBound.signum();
		int upperCompare = upperBound.signum();
		if (lowerCompare >= 0 && upperCompare >= 0) {
			return Positivity.POSITIVE;
		}
		if (lowerCompare <= 0 && upperCompare <= 0) {
			return Positivity.NEGATIVE;
		}
		return isError() ? Positivity.ZERO_IMPROPER : Positivity.ZERO_PROPER;
	}

	private enum Positivity {
		POSITIVE,
		NEGATIVE,
		ZERO_PROPER,
		ZERO_IMPROPER
	}
}
