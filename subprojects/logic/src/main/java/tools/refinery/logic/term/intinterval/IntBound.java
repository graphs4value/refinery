/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.intinterval;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

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
				return signum < 0 ? roundingMode.negativeInfinity() : roundingMode.positiveInfinity();
			}

			public IntBound div(IntBound other, RoundingMode roundingMode) {
				var signum = other.signum();
				if (signum == 0) {
					throw new ArithmeticException();
				}
				return signum < 0 ? roundingMode.negativeInfinity() : roundingMode.positiveInfinity();
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
				return signum < 0 ? roundingMode.positiveInfinity() : roundingMode.negativeInfinity();
			}

			public IntBound div(IntBound other, RoundingMode roundingMode) {
				var signum = other.signum();
				if (signum == 0) {
					throw new ArithmeticException();
				}
				return signum < 0 ? roundingMode.positiveInfinity() : roundingMode.negativeInfinity();
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

	record Finite(int value) implements IntBound {
		public static final Finite ZERO = new Finite(0);
		public static final Finite ONE = new Finite(1);
		public static final Finite NEGATIVE_ONE = new Finite(-1);
		public static final Finite MIN_VALUE = new Finite(Integer.MIN_VALUE);
		public static final Finite MAX_VALUE = new Finite(Integer.MAX_VALUE);

		public static final BigDecimal MIN_DECIMAL_VALUE = BigDecimal.valueOf(MIN_VALUE.value);
		public static final BigDecimal MAX_DECIMAL_VALUE = BigDecimal.valueOf(MAX_VALUE.value);

		@Override
		public boolean lessThanOrEquals(IntBound other) {
			return switch (other) {
				case Infinite.POSITIVE_INFINITY -> true;
				case Infinite.NEGATIVE_INFINITY -> false;
				case Finite(int otherValue) -> value <= otherValue;
			};
		}

		@Override
		public boolean isFinite() {
			return true;
		}

		@Override
		public IntBound minus(RoundingMode roundingMode) {
			if (value == Integer.MIN_VALUE) {
				return roundingMode.positiveInfinity();
			}
			return new Finite(-value);
		}

		@Override
		public IntBound add(IntBound other, RoundingMode roundingMode) {
			return switch (other) {
				case Infinite.POSITIVE_INFINITY -> Infinite.POSITIVE_INFINITY;
				case Infinite.NEGATIVE_INFINITY -> Infinite.NEGATIVE_INFINITY;
				case Finite(int otherValue) -> {
					int sum = value + otherValue;
					int sign = Integer.signum(value);
					int otherSign = Integer.signum(otherValue);
					if (sign == otherSign && sign != Integer.signum(sum)) {
						yield sign > 0 ? roundingMode.positiveInfinity() : roundingMode.negativeInfinity();
					}
					yield new Finite(sum);
				}
			};
		}

		@Override
		public IntBound sub(IntBound other, RoundingMode roundingMode) {
			return switch (other) {
				case Infinite.POSITIVE_INFINITY -> Infinite.NEGATIVE_INFINITY;
				case Infinite.NEGATIVE_INFINITY -> Infinite.POSITIVE_INFINITY;
				case Finite(int otherValue) -> {
					int diff = value - otherValue;
					int sign = Integer.signum(value);
					int otherSign = Integer.signum(otherValue);
					if (sign != otherSign && sign != Integer.signum(diff)) {
						yield sign > 0 ? roundingMode.positiveInfinity() : roundingMode.negativeInfinity();
					}
					yield new Finite(diff);
				}
			};
		}

		@Override
		public IntBound mul(IntBound other, RoundingMode roundingMode) {
			return switch (other) {
				case Infinite ignored -> other.mul(this, roundingMode);
				case Finite(int otherValue) -> {
					long longResult = (long) value * (long) otherValue;
					int result = (int) longResult;
					if ((long) result != longResult) {
						yield Integer.signum(value) * Integer.signum(otherValue) > 0 ?
								roundingMode.positiveInfinity() : roundingMode.negativeInfinity();
					}
					yield new Finite(result);
				}
			};
		}

		@Override
		public IntBound div(IntBound other, RoundingMode roundingMode) {
			return switch (other) {
				case Infinite ignored -> ZERO;
				case Finite(int otherValue) -> new Finite(value / otherValue);
			};
		}

		@Override
		public int signum() {
			return Integer.signum(value);
		}

		@Override
		public @NotNull String toString() {
			return String.valueOf(value);
		}

		@Override
		public int compareBound(IntBound other) {
			return other instanceof Finite(int otherValue) ? Integer.compare(value, otherValue) :
					-other.compareBound(this);
		}
	}

	static IntBound of(int value) {
		return new Finite(value);
	}

	static IntBound of(BigDecimal value, RoundingMode roundingMode) {
		// Casting to {@code int} in Java rounds towards zero.
		var rounded = value.setScale(0, java.math.RoundingMode.DOWN);
		if (rounded.compareTo(Finite.MIN_DECIMAL_VALUE) < 0) {
			return roundingMode.negativeInfinity();
		}
		if (rounded.compareTo(Finite.MAX_DECIMAL_VALUE) > 0) {
			return roundingMode.positiveInfinity();
		}
		return IntBound.of(rounded.intValueExact());
	}
}
