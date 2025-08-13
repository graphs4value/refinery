/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.intinterval;

import org.jetbrains.annotations.NotNull;

public sealed interface Bound {
	boolean lessThanOrEquals(Bound other);

	default boolean greaterThanOrEquals(Bound other) {
		return other.lessThanOrEquals(this);
	}

	boolean isFinite();

	default Bound min(Bound other) {
		return lessThanOrEquals(other) ? this : other;
	}

	default Bound max(Bound other) {
		return greaterThanOrEquals(other) ? this : other;
	}

	Bound minus();

	Bound add(Bound other, Infinite errorTowards);

	Bound sub(Bound other, Infinite errorTowards);

	Bound mul(Bound other);

	Bound div(Bound other);

	int signum();

	int compareBound(Bound other);

	enum Infinite implements Bound {
		POSITIVE_INFINITY {
			@Override
			public boolean lessThanOrEquals(Bound other) {
				return this == other;
			}

			@Override
			public Bound minus() {
				return NEGATIVE_INFINITY;
			}

			@Override
			public Bound add(Bound other, Infinite errorTowards) {
				return other == NEGATIVE_INFINITY ? errorTowards : POSITIVE_INFINITY;
			}

			@Override
			public Bound sub(Bound other, Infinite errorTowards) {
				return other == POSITIVE_INFINITY ? errorTowards : POSITIVE_INFINITY;
			}

			@Override
			public Bound mul(Bound other) {
				var signum = other.signum();
				if (signum == 0) {
					return Finite.ZERO;
				}
				return signum < 0 ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
			}

			public Bound div(Bound other) {
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
			public int compareBound(Bound other) {
				return other == POSITIVE_INFINITY ? 0 : -1;
			}
		},

		NEGATIVE_INFINITY {
			@Override
			public boolean lessThanOrEquals(Bound other) {
				return true;
			}

			@Override
			public Bound minus() {
				return NEGATIVE_INFINITY;
			}

			@Override
			public Bound add(Bound other, Infinite errorTowards) {
				return other == POSITIVE_INFINITY ? errorTowards : NEGATIVE_INFINITY;
			}

			@Override
			public Bound sub(Bound other, Infinite errorTowards) {
				return other == NEGATIVE_INFINITY ? errorTowards : NEGATIVE_INFINITY;
			}

			@Override
			public Bound mul(Bound other) {
				var signum = other.signum();
				if (signum == 0) {
					return Finite.ZERO;
				}
				return signum < 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
			}

			public Bound div(Bound other) {
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
			public int compareBound(Bound other) {
				return other == NEGATIVE_INFINITY ? 0 : 1;
			}
		};

		@Override
		public boolean isFinite() {
			return false;
		}
	}

	record Finite(int value) implements Bound {
		public static final Finite ZERO = new Finite(0);
		public static final Finite ONE = new Finite(1);
		public static final Finite NEGATIVE_ONE = new Finite(-1);

		@Override
		public boolean lessThanOrEquals(Bound other) {
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
		public Bound minus() {
			return new Finite(-value);
		}

		@Override
		public Bound add(Bound other, Infinite errorTowards) {
			return switch (other) {
				case Infinite.POSITIVE_INFINITY -> Infinite.POSITIVE_INFINITY;
				case Infinite.NEGATIVE_INFINITY -> Infinite.NEGATIVE_INFINITY;
				case Finite(int otherValue) -> {
					int sum = value + otherValue;
					int sign = Integer.signum(value);
					int otherSign = Integer.signum(otherValue);
					if (sign == otherSign && sign != Integer.signum(sum)) {
						yield sign > 0 ? Infinite.POSITIVE_INFINITY : Infinite.NEGATIVE_INFINITY;
					}
					yield new Finite(sum);
				}
			};
		}

		@Override
		public Bound sub(Bound other, Infinite errorTowards) {
			return switch (other) {
				case Infinite.POSITIVE_INFINITY -> Infinite.NEGATIVE_INFINITY;
				case Infinite.NEGATIVE_INFINITY -> Infinite.POSITIVE_INFINITY;
				case Finite(int otherValue) -> {
					int diff = value - otherValue;
					int sign = Integer.signum(value);
					int otherSign = Integer.signum(otherValue);
					if (sign != otherSign && sign != Integer.signum(diff)) {
						yield sign > 0 ? Infinite.POSITIVE_INFINITY : Infinite.NEGATIVE_INFINITY;
					}
					yield new Finite(diff);
				}
			};
		}

		@Override
		public Bound mul(Bound other) {
			return switch (other) {
				case Infinite ignored -> other.mul(this);
				case Finite(int otherValue) -> {
					long longResult = (long) value * (long) otherValue;
					int result = (int) longResult;
					if ((long) result != longResult) {
						yield Integer.signum(value) * Integer.signum(otherValue) > 0 ? Infinite.POSITIVE_INFINITY :
								Infinite.NEGATIVE_INFINITY;
					}
					yield new Finite(result);
				}
			};
		}

		@Override
		public Bound div(Bound other) {
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
		public int compareBound(Bound other) {
			return other instanceof Finite(int otherValue) ? Integer.compare(value, otherValue) :
					-other.compareBound(this);
		}
	}

	static Bound of(int value) {
		return new Finite(value);
	}
}
