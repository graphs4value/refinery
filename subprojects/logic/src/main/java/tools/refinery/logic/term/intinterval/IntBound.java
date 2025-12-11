/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.intinterval;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;

public sealed interface IntBound {
	boolean lessThanOrEquals(IntBound other);

	default boolean greaterThanOrEquals(IntBound other) {
		return other.lessThanOrEquals(this);
	}

	boolean isFinite();

	default IntBound min(IntBound other) {
		return lessThanOrEquals(other) ? this : other;
	}

	default IntBound max(IntBound other) {
		return greaterThanOrEquals(other) ? this : other;
	}

	IntBound minus(RoundingMode roundingMode);

	IntBound add(IntBound other, RoundingMode roundingMode);

	IntBound sub(IntBound other, RoundingMode roundingMode);

	IntBound mul(IntBound other, RoundingMode roundingMode);

	IntBound div(IntBound other, RoundingMode roundingMode);

	int signum();

	int compareBound(IntBound other);

	enum Infinite implements IntBound {
		POSITIVE_INFINITY {
			@Override
			public boolean lessThanOrEquals(IntBound other) {
				return this == other;
			}

			@Override
			public IntBound minus(RoundingMode roundingMode) {
				return NEGATIVE_INFINITY;
			}

			@Override
			public IntBound add(IntBound other, RoundingMode roundingMode) {
				return other == NEGATIVE_INFINITY ? roundingMode.error() : POSITIVE_INFINITY;
			}

			@Override
			public IntBound sub(IntBound other, RoundingMode roundingMode) {
				return other == POSITIVE_INFINITY ? roundingMode.error() : POSITIVE_INFINITY;
			}

			@Override
			public IntBound mul(IntBound other, RoundingMode roundingMode) {
				var signum = other.signum();
				if (signum == 0) {
					return Finite.ZERO;
				}
				return signum < 0 ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
			}

			public IntBound div(IntBound other, RoundingMode roundingMode) {
				var signum = other.signum();
				if (signum == 0) {
					throw new ArithmeticException();
				}
				return signum < 0 ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
			}

			@Override
			public int signum() {
				return 1;
			}

			@Override
			public String toString() {
				return "∞";
			}

			@Override
			public int compareBound(IntBound other) {
				return other == POSITIVE_INFINITY ? 0 : -1;
			}
		},

		NEGATIVE_INFINITY {
			@Override
			public boolean lessThanOrEquals(IntBound other) {
				return true;
			}

			@Override
			public IntBound minus(RoundingMode roundingMode) {
				return NEGATIVE_INFINITY;
			}

			@Override
			public IntBound add(IntBound other, RoundingMode roundingMode) {
				return other == POSITIVE_INFINITY ? roundingMode.error() : NEGATIVE_INFINITY;
			}

			@Override
			public IntBound sub(IntBound other, RoundingMode roundingMode) {
				return other == NEGATIVE_INFINITY ? roundingMode.error() : NEGATIVE_INFINITY;
			}

			@Override
			public IntBound mul(IntBound other, RoundingMode roundingMode) {
				var signum = other.signum();
				if (signum == 0) {
					return Finite.ZERO;
				}
				return signum < 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
			}

			public IntBound div(IntBound other, RoundingMode roundingMode) {
				var signum = other.signum();
				if (signum == 0) {
					throw new ArithmeticException();
				}
				return signum < 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
			}

			@Override
			public int signum() {
				return -1;
			}

			@Override
			public String toString() {
				return "-∞";
			}

			@Override
			public int compareBound(IntBound other) {
				return other == NEGATIVE_INFINITY ? 0 : 1;
			}
		};

		@Override
		public boolean isFinite() {
			return false;
		}
	}

	record Finite(BigInteger value) implements IntBound {
		public static final Finite ZERO = new Finite(BigInteger.ZERO);
		public static final Finite ONE = new Finite(BigInteger.ONE);
		public static final Finite NEGATIVE_ONE = new Finite(BigInteger.valueOf(-1));

		@Override
		public boolean lessThanOrEquals(IntBound other) {
			return switch (other) {
				case Infinite.POSITIVE_INFINITY -> true;
				case Infinite.NEGATIVE_INFINITY -> false;
				case Finite(var otherValue) -> value.compareTo(otherValue) <= 0;
			};
		}

		@Override
		public boolean isFinite() {
			return true;
		}

		@Override
		public IntBound minus(RoundingMode roundingMode) {
			return new Finite(value.negate());
		}

		@Override
		public IntBound add(IntBound other, RoundingMode roundingMode) {
			return switch (other) {
				case Infinite.POSITIVE_INFINITY -> Infinite.POSITIVE_INFINITY;
				case Infinite.NEGATIVE_INFINITY -> Infinite.NEGATIVE_INFINITY;
				case Finite(var otherValue) -> new Finite(value.add(otherValue));
			};
		}

		@Override
		public IntBound sub(IntBound other, RoundingMode roundingMode) {
			return switch (other) {
				case Infinite.POSITIVE_INFINITY -> Infinite.NEGATIVE_INFINITY;
				case Infinite.NEGATIVE_INFINITY -> Infinite.POSITIVE_INFINITY;
				case Finite(var otherValue) -> new Finite(value.subtract(otherValue));
			};
		}

		@Override
		public IntBound mul(IntBound other, RoundingMode roundingMode) {
			return switch (other) {
				case Infinite ignored -> other.mul(this, roundingMode);
				case Finite(var otherValue) -> new Finite(value.multiply(otherValue));
			};
		}

		@Override
		public IntBound div(IntBound other, RoundingMode roundingMode) {
			return switch (other) {
				case Infinite ignored -> ZERO;
				case Finite(var otherValue) -> new Finite(value.divide(otherValue));
			};
		}

		@Override
		public int signum() {
			return value.signum();
		}

		@Override
		public @NotNull String toString() {
			return value.toString();
		}

		@Override
		public int compareBound(IntBound other) {
			return other instanceof Finite(var otherValue) ? value.compareTo(otherValue) :
					-other.compareBound(this);
		}
	}

	static IntBound of(BigInteger value) {
		return new Finite(value);
	}

	static IntBound of(int value) {
		return new Finite(BigInteger.valueOf(value));
	}

	static IntBound of(BigDecimal value) {
		// Casting to {@code int} in Java rounds towards zero.
		var rounded = value.setScale(0, java.math.RoundingMode.DOWN);
		return IntBound.of(rounded.toBigIntegerExact());
	}
}
