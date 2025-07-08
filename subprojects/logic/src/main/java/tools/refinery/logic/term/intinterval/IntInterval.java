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

public record IntInterval(@NotNull Bound lowerBound, @NotNull Bound upperBound)
		implements ComparableAbstractValue<IntInterval, Integer>, Plus<IntInterval>, Minus<IntInterval>,
		Add<IntInterval>, Sub<IntInterval>, Mul<IntInterval> {
	public static final IntInterval ZERO = new IntInterval(Bound.Finite.ZERO, Bound.Finite.ZERO);
	public static final IntInterval UNKNOWN = new IntInterval(Bound.Infinite.NEGATIVE_INFINITY,
			Bound.Infinite.POSITIVE_INFINITY);
	public static final IntInterval ERROR = new IntInterval(Bound.Infinite.POSITIVE_INFINITY,
			Bound.Infinite.NEGATIVE_INFINITY);
	public static final IntInterval NEGATIVE_INFINITY = new IntInterval(Bound.Infinite.NEGATIVE_INFINITY,
			Bound.Infinite.NEGATIVE_INFINITY);
	public static final IntInterval POSITIVE_INFINITY = new IntInterval(Bound.Infinite.POSITIVE_INFINITY,
			Bound.Infinite.POSITIVE_INFINITY);

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
		return new IntInterval(lowerBound.min(other.lowerBound), upperBound.max(other.upperBound));
	}

	@Override
	public IntInterval meet(IntInterval other) {
		return new IntInterval(lowerBound.max(other.lowerBound), upperBound.min(other.upperBound));
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
	public @NotNull String toString() {
		if (lowerBound.equals(upperBound)) {
			return lowerBound().toString();
		}
		if (Bound.Infinite.NEGATIVE_INFINITY.equals(lowerBound) &&
				Bound.Infinite.POSITIVE_INFINITY.equals(upperBound)) {
			return "unknown";
		}
		if (Bound.Infinite.POSITIVE_INFINITY.equals(lowerBound) &&
				Bound.Infinite.NEGATIVE_INFINITY.equals(upperBound)) {
			return "error";
		}
		var builder = new StringBuilder();
		if (Bound.Infinite.NEGATIVE_INFINITY.equals(lowerBound)) {
			builder.append("*");
		} else {
			builder.append(lowerBound);
		}
		builder.append("..");
		if (Bound.Infinite.POSITIVE_INFINITY.equals(upperBound)) {
			builder.append("*");
		} else {
			builder.append(upperBound);
		}
		return builder.toString();
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
		return of(lowerBound().add(other.lowerBound(), Bound.Infinite.POSITIVE_INFINITY),
				upperBound().add(other.upperBound(), Bound.Infinite.NEGATIVE_INFINITY));
	}

	@Override
	public IntInterval sub(IntInterval other) {
		return of(lowerBound().sub(other.upperBound(), Bound.Infinite.POSITIVE_INFINITY),
				upperBound().sub(other.lowerBound(), Bound.Infinite.NEGATIVE_INFINITY));
	}

	private boolean isZero() {
		return Bound.Finite.ZERO.equals(lowerBound) && Bound.Finite.ZERO.equals(upperBound);
	}

	@Override
	public IntInterval mul(IntInterval other) {
		if (isZero() || other.isZero()) {
			return ZERO;
		}
		var lowerBound = lowerBound();
		var upperBound = upperBound();
		var otherLowerBound = other.lowerBound();
		var otherUpperBound = other.upperBound();
		if (lowerBound.signum() >= 0) {
			if (otherLowerBound.signum() >= 0) {
				return of(lowerBound.mul(otherLowerBound), upperBound.mul(otherUpperBound));
			}
			if (otherUpperBound.signum() <= 0) {
				return of(upperBound.mul(otherLowerBound), lowerBound.mul(otherUpperBound));
			}
			return of(upperBound.mul(otherLowerBound), upperBound.mul(otherUpperBound));
		}
		if (upperBound.signum() <= 0) {
			if (otherLowerBound.signum() >= 0) {
				return of(lowerBound.mul(otherUpperBound), upperBound.mul(otherLowerBound));
			}
			if (otherUpperBound.signum() <= 0) {
				return of(upperBound.mul(otherUpperBound), lowerBound.mul(otherLowerBound));
			}
			return of(lowerBound.mul(otherUpperBound), lowerBound.mul(otherLowerBound));
		}
		if (otherLowerBound.signum() >= 0) {
			return of(lowerBound.mul(otherUpperBound), upperBound.mul(otherUpperBound));
		}
		if (otherUpperBound.signum() <= 0) {
			return of(upperBound.mul(otherLowerBound), lowerBound.mul(otherLowerBound));
		}
		var newLowerBound = upperBound.mul(otherLowerBound).min(lowerBound.mul(otherUpperBound));
		var newUpperBound = lowerBound.mul(otherLowerBound).max(upperBound.mul(otherUpperBound));
		return of(newLowerBound, newUpperBound);
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
}
