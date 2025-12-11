/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.realinterval;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.term.ComparableAbstractValue;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.operators.*;
import tools.refinery.logic.term.truthvalue.TruthValue;

import java.math.BigDecimal;

import static tools.refinery.logic.term.realinterval.RoundingMode.CEIL;
import static tools.refinery.logic.term.realinterval.RoundingMode.FLOOR;

public record RealInterval(@NotNull RealBound lowerBound, @NotNull RealBound upperBound)
		implements ComparableAbstractValue<RealInterval, BigDecimal>, Comparable<RealInterval>, Plus<RealInterval>,
		Minus<RealInterval>, Add<RealInterval>, Sub<RealInterval>, Mul<RealInterval>, Div<RealInterval>,
		Exp<RealInterval>, Log<RealInterval>, Sqrt<RealInterval>, Pow<RealInterval> {
	public static final RealInterval ZERO = new RealInterval(RealBound.Finite.ZERO, RealBound.Finite.ZERO);
	public static final RealInterval ONE = new RealInterval(RealBound.Finite.ONE, RealBound.Finite.ONE);
	public static final RealInterval UNKNOWN = new RealInterval(RealBound.Infinite.NEGATIVE_INFINITY,
			RealBound.Infinite.POSITIVE_INFINITY);
	public static final RealInterval ERROR = new RealInterval(RealBound.Infinite.POSITIVE_INFINITY,
			RealBound.Infinite.NEGATIVE_INFINITY);
	public static final RealInterval POSITIVE_ERROR = new RealInterval(RealBound.Infinite.POSITIVE_INFINITY,
			RealBound.Finite.ZERO);
	public static final RealInterval NEGATIVE_INFINITY = new RealInterval(RealBound.Infinite.NEGATIVE_INFINITY,
			RealBound.Infinite.NEGATIVE_INFINITY);
	public static final RealInterval POSITIVE_INFINITY = new RealInterval(RealBound.Infinite.POSITIVE_INFINITY,
			RealBound.Infinite.POSITIVE_INFINITY);

	@Override
	public @Nullable BigDecimal getConcrete() {
		if (lowerBound.equals(upperBound) && lowerBound instanceof RealBound.Finite(var value)) {
			return value;
		}
		return null;
	}

	@Override
	public boolean isConcrete() {
		return lowerBound.equals(upperBound) && lowerBound.isFinite();
	}

	@Override
	public @Nullable BigDecimal getArbitrary() {
		if (isError()) {
			return null;
		}
		if (lowerBound instanceof RealBound.Finite(var value)) {
			return value;
		}
		if (upperBound instanceof RealBound.Finite(var value)) {
			return value;
		}
		return BigDecimal.ZERO;
	}

	@Override
	public boolean isError() {
		if (lowerBound.lessThanOrEquals(upperBound)) {
			return lowerBound == RealBound.Infinite.POSITIVE_INFINITY ||
					upperBound == RealBound.Infinite.NEGATIVE_INFINITY;
		}
		return true;
	}

	@Override
	public RealInterval join(RealInterval other) {
		return new RealInterval(lowerBound.min(other.lowerBound), upperBound.max(other.upperBound));
	}

	@Override
	public RealInterval meet(RealInterval other) {
		return new RealInterval(lowerBound.max(other.lowerBound), upperBound.min(other.upperBound));
	}

	public static RealInterval of(BigDecimal value) {
		var bound = RealBound.of(value);
		return of(bound, bound);
	}

	public static RealInterval of(String value) {
		return of(new BigDecimal(value));
	}

	public static RealInterval of(BigDecimal value1, BigDecimal value2) {
		var bound1 = RealBound.of(value1);
		var bound2 = RealBound.of(value2);
		return of(bound1, bound2);
	}

	public static RealInterval of(String value1, String value2) {
		return of(new BigDecimal(value1), new BigDecimal(value2));
	}

	public static RealInterval of(BigDecimal value, RealBound bound) {
		var valueBound = RealBound.of(value);
		return of(valueBound, bound);
	}

	public static RealInterval of(String value, RealBound bound) {
		return of(new BigDecimal(value), bound);
	}

	public static RealInterval of(RealBound bound, BigDecimal value) {
		var valueBound = RealBound.of(value);
		return of(bound, valueBound);
	}

	public static RealInterval of(RealBound bound, String value) {
		return of(bound, new BigDecimal(value));
	}

	public static RealInterval of(RealBound bound1, RealBound bound2) {
		return new RealInterval(bound1.round(FLOOR), bound2.round(CEIL));
	}

	public static RealInterval fromInt(IntInterval intInterval) {
		return of(RealBound.fromInt(intInterval.lowerBound(), FLOOR),
				RealBound.fromInt(intInterval.upperBound(), CEIL));
	}

	public IntInterval asInt() {
		return IntInterval.of(lowerBound.asInt(), upperBound.asInt());
	}

	@Override
	public @NotNull String toString() {
		if (lowerBound.equals(upperBound)) {
			return lowerBound().toString();
		}
		if (RealBound.Infinite.NEGATIVE_INFINITY.equals(lowerBound) &&
				RealBound.Infinite.POSITIVE_INFINITY.equals(upperBound)) {
			return "unknown";
		}
		if (RealBound.Infinite.POSITIVE_INFINITY.equals(lowerBound) &&
				RealBound.Infinite.NEGATIVE_INFINITY.equals(upperBound)) {
			return "error";
		}
		var builder = new StringBuilder();
		if (RealBound.Infinite.NEGATIVE_INFINITY.equals(lowerBound)) {
			builder.append("*");
		} else {
			builder.append(lowerBound);
		}
		builder.append("..");
		if (RealBound.Infinite.POSITIVE_INFINITY.equals(upperBound)) {
			builder.append("*");
		} else {
			builder.append(upperBound);
		}
		return builder.toString();
	}

	@Override
	public int compareTo(@NotNull RealInterval other) {
		int result = lowerBound.compareBound(other.lowerBound);
		return result == 0 ? upperBound.compareBound(other.upperBound) : result;
	}

	@Override
	public RealInterval plus() {
		return this;
	}

	@Override
	public RealInterval minus() {
		return of(upperBound().minus(FLOOR), lowerBound().minus(CEIL));
	}

	@Override
	public RealInterval add(RealInterval other) {
		return of(lowerBound().add(other.lowerBound(), FLOOR),
				upperBound().add(other.upperBound(), CEIL));
	}

	@Override
	public RealInterval sub(RealInterval other) {
		return of(lowerBound().sub(other.upperBound(), FLOOR),
				upperBound().sub(other.lowerBound(), CEIL));
	}

	@Override
	public RealInterval mul(RealInterval other) {
		// Kaucher, E. (1980). Interval Analysis in the Extended Interval Space IR. In: Fundamentals of Numerical
		// Computation (Computer-Oriented Numerical Analysis). Springer. https://doi.org/10.1007/978-3-7091-8577-3_3
		var otherLowerBound = other.lowerBound();
		var otherUpperBound = other.upperBound();
		return switch (positivity()) {
			case POSITIVE -> switch (other.positivity()) {
				case POSITIVE -> of(lowerBound.mul(otherLowerBound, FLOOR), upperBound.mul(otherUpperBound, CEIL));
				case ZERO_PROPER -> of(upperBound.mul(otherLowerBound, FLOOR), upperBound.mul(otherUpperBound, CEIL));
				case NEGATIVE -> of(upperBound.mul(otherLowerBound, FLOOR), lowerBound.mul(otherUpperBound, CEIL));
				case ZERO_IMPROPER -> of(lowerBound.mul(otherLowerBound, FLOOR),
						lowerBound.mul(otherUpperBound, CEIL));
			};
			case ZERO_PROPER -> switch (other.positivity()) {
				case POSITIVE -> of(lowerBound.mul(otherUpperBound, FLOOR), upperBound.mul(otherUpperBound, CEIL));
				case ZERO_PROPER -> of(
						lowerBound.mul(otherUpperBound, FLOOR).min(upperBound.mul(otherLowerBound, FLOOR)),
						lowerBound.mul(otherLowerBound, CEIL).max(upperBound.mul(otherUpperBound, CEIL))
				);
				case NEGATIVE -> of(upperBound.mul(otherLowerBound, FLOOR), lowerBound.mul(otherLowerBound, CEIL));
				case ZERO_IMPROPER -> RealInterval.ZERO;
			};
			case NEGATIVE -> switch (other.positivity()) {
				case POSITIVE -> of(lowerBound.mul(otherUpperBound, FLOOR), upperBound.mul(otherLowerBound, CEIL));
				case ZERO_PROPER -> of(lowerBound.mul(otherUpperBound, FLOOR), lowerBound.mul(otherLowerBound, CEIL));
				case NEGATIVE -> of(upperBound.mul(otherUpperBound, FLOOR), lowerBound.mul(otherLowerBound, CEIL));
				case ZERO_IMPROPER -> of(upperBound.mul(otherUpperBound, FLOOR),
						upperBound.mul(otherLowerBound, CEIL));
			};
			case ZERO_IMPROPER -> switch (other.positivity()) {
				case POSITIVE -> of(lowerBound.mul(otherLowerBound, FLOOR), upperBound.mul(otherLowerBound, CEIL));
				case ZERO_PROPER -> RealInterval.ZERO;
				case NEGATIVE -> of(upperBound.mul(otherUpperBound, FLOOR), lowerBound.mul(otherUpperBound, CEIL));
				case ZERO_IMPROPER -> of(
						lowerBound.mul(otherLowerBound, FLOOR).max(upperBound.mul(otherUpperBound, FLOOR)),
						lowerBound.mul(otherUpperBound, CEIL).min(upperBound.mul(otherLowerBound, CEIL))
				);
			};
		};
	}

	@Override
	public RealInterval div(RealInterval other) {
		var otherLowerBound = other.lowerBound();
		var otherUpperBound = other.upperBound();
		if (ZERO.equals(this)) {
			return ZERO;
		}
		RealInterval negativeResult = null;
		if (otherLowerBound.signum() < 0) {
			var negativeDivisor = RealInterval.of(otherLowerBound, otherUpperBound.min(RealBound.Finite.ZERO));
			negativeResult = divWithNegative(negativeDivisor);
		}
		if (otherUpperBound.signum() > 0) {
			var positiveDivisor = RealInterval.of(otherLowerBound.max(RealBound.Finite.ZERO), otherUpperBound);
			var positiveResult = divWithPositive(positiveDivisor);
			return negativeResult == null ? positiveResult : positiveResult.join(negativeResult);
		}
		return negativeResult == null ? RealInterval.ERROR : negativeResult;
	}

	private RealInterval divWithNegative(RealInterval other) {
		var otherLowerBound = other.lowerBound();
		var otherUpperBound = other.upperBound();
		return switch (positivity()) {
			case POSITIVE -> of(upperBound.div(otherUpperBound, FLOOR), lowerBound.div(otherLowerBound, CEIL));
			case ZERO_PROPER -> of(upperBound.div(otherUpperBound, FLOOR), lowerBound.div(otherUpperBound, CEIL));
			case NEGATIVE -> of(upperBound.div(otherLowerBound, FLOOR), lowerBound.div(otherUpperBound, CEIL));
			case ZERO_IMPROPER -> of(upperBound.div(otherLowerBound, FLOOR), lowerBound.div(otherLowerBound, CEIL));
		};
	}

	private RealInterval divWithPositive(RealInterval other) {
		var otherLowerBound = other.lowerBound();
		var otherUpperBound = other.upperBound();
		return switch (positivity()) {
			case POSITIVE -> of(lowerBound.div(otherUpperBound, FLOOR), upperBound.div(otherLowerBound, CEIL));
			case ZERO_PROPER -> of(lowerBound.div(otherLowerBound, FLOOR), upperBound.div(otherLowerBound, CEIL));
			case NEGATIVE -> of(lowerBound.div(otherLowerBound, FLOOR), upperBound.div(otherUpperBound, CEIL));
			case ZERO_IMPROPER -> of(lowerBound.div(otherUpperBound, FLOOR), upperBound.div(otherUpperBound, CEIL));
		};
	}

	@Override
	public RealInterval exp() {
		return of(lowerBound.exp(FLOOR), upperBound.exp(CEIL));
	}

	@Override
	public RealInterval log() {
		if (upperBound.signum() < 0) {
			return RealInterval.ERROR;
		}
		var clampedLowerBound = lowerBound().max(RealBound.Finite.ZERO);
		return of(clampedLowerBound.log(FLOOR), upperBound.log(CEIL));
	}

	@Override
	public RealInterval sqrt() {
		if (upperBound.signum() < 0) {
			return RealInterval.POSITIVE_ERROR;
		}
		var clampedLowerBound = lowerBound().max(RealBound.Finite.ZERO);
		return of(clampedLowerBound.sqrt(FLOOR), upperBound.sqrt(CEIL));
	}

	@Override
	public RealInterval pow(RealInterval other) {
		int upperBoundSign = upperBound.signum();
		if (upperBoundSign < 0) {
			// Trying to exponentiate a surely negative number.
			return RealInterval.POSITIVE_ERROR;
		}
		if (upperBoundSign == 0) {
			// When the only non-negative number in the interval is 0, negative exponents are forbidden.
			// If the interval contains positive numbers, 0 is treated as an infinitesimally small (but positive)
			// number in {@link RealBound#pow(RealBound, RoundingMode)}.
			if (other.upperBound.signum() < 0) {
				return RealInterval.POSITIVE_ERROR;
			}
			other = of(other.lowerBound.max(RealBound.Finite.ZERO), other.upperBound);
		}
		var clampedLowerBound = lowerBound.max(RealBound.Finite.ZERO);
		RealInterval antiMonotoneResult = null;
		if (clampedLowerBound.compareBound(RealBound.Finite.ONE) <= 0) {
			antiMonotoneResult = powLessThan1(clampedLowerBound, upperBound.min(RealBound.Finite.ONE), other);
		}
		if (upperBound.compareBound(RealBound.Finite.ONE) >= 0) {
			var monotoneResult = powGreaterThan1(clampedLowerBound.max(RealBound.Finite.ONE), upperBound, other);
			return antiMonotoneResult == null ? monotoneResult : monotoneResult.join(antiMonotoneResult);
		}
		return antiMonotoneResult == null ? POSITIVE_ERROR : antiMonotoneResult;
	}

	private static RealInterval powLessThan1(RealBound lowerBound, RealBound upperBound, RealInterval exponent) {
		var otherLowerBound = exponent.lowerBound();
		var otherUpperBound = exponent.upperBound();
		return switch (exponent.positivity()) {
			case POSITIVE -> of(lowerBound.pow(otherUpperBound, FLOOR), upperBound.pow(otherLowerBound, CEIL));
			case ZERO_PROPER -> of(lowerBound.pow(otherUpperBound, FLOOR), lowerBound.pow(otherLowerBound, CEIL));
			case NEGATIVE -> of(upperBound.pow(otherUpperBound, FLOOR), lowerBound.pow(otherLowerBound, CEIL));
			case ZERO_IMPROPER -> of(upperBound.pow(otherUpperBound, FLOOR),
					upperBound.pow(otherLowerBound, CEIL));
		};
	}

	private static RealInterval powGreaterThan1(RealBound lowerBound, RealBound upperBound, RealInterval exponent) {
		var otherLowerBound = exponent.lowerBound();
		var otherUpperBound = exponent.upperBound();
		return switch (exponent.positivity()) {
			case POSITIVE -> of(lowerBound.pow(otherLowerBound, FLOOR), upperBound.pow(otherUpperBound, CEIL));
			case ZERO_PROPER -> of(upperBound.pow(otherLowerBound, FLOOR), upperBound.pow(otherUpperBound, CEIL));
			case NEGATIVE -> of(upperBound.pow(otherLowerBound, FLOOR), lowerBound.pow(otherUpperBound, CEIL));
			case ZERO_IMPROPER -> of(lowerBound.pow(otherLowerBound, FLOOR),
					lowerBound.pow(otherUpperBound, CEIL));
		};
	}

	@Override
	public TruthValue checkEquals(RealInterval other) {
		return checkLessEq(other).and(other.checkLessEq(this));
	}

	@Override
	public TruthValue checkLess(RealInterval other) {
		return other.checkLessEq(this).not();
	}

	@Override
	public TruthValue checkLessEq(RealInterval other) {
		var may = lowerBound().lessThanOrEquals(other.upperBound());
		var must = upperBound().lessThanOrEquals(other.lowerBound());
		return TruthValue.of(may, must);
	}

	@Override
	public RealInterval upToIncluding(RealInterval other) {
		return RealInterval.of(lowerBound(), other.upperBound());
	}

	@Override
	public RealInterval min(RealInterval other) {
		return of(lowerBound().min(other.lowerBound()), upperBound().min(other.upperBound()));
	}

	@Override
	public RealInterval max(RealInterval other) {
		return of(lowerBound().max(other.lowerBound()), upperBound().max(other.upperBound()));
	}

	@Override
	public RealInterval abstractLowerBound() {
		return RealInterval.of(lowerBound, lowerBound);
	}

	@Override
	public RealInterval abstractUpperBound() {
		return RealInterval.of(upperBound, upperBound);
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
