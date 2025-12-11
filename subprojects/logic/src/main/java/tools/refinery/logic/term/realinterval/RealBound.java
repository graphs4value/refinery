/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.realinterval;

import ch.obermuhlner.math.big.BigDecimalMath;
import org.jetbrains.annotations.NotNull;
import tools.refinery.logic.term.intinterval.IntBound;

import java.math.BigDecimal;
import java.util.Objects;

public sealed interface RealBound {
	boolean lessThanOrEquals(RealBound other);

	default boolean greaterThanOrEquals(RealBound other) {
		return other.lessThanOrEquals(this);
	}

	boolean isFinite();

	default RealBound min(RealBound other) {
		return lessThanOrEquals(other) ? this : other;
	}

	default RealBound max(RealBound other) {
		return greaterThanOrEquals(other) ? this : other;
	}

	RealBound round(RoundingMode roundingMode);

	RealBound minus(RoundingMode roundingMode);

	RealBound add(RealBound other, RoundingMode roundingMode);

	RealBound sub(RealBound other, RoundingMode roundingMode);

	RealBound mul(RealBound other, RoundingMode roundingMode);

	RealBound div(RealBound other, RoundingMode roundingMode);

	RealBound exp(RoundingMode roundingMode);

	RealBound log(RoundingMode roundingMode);

	RealBound sqrt(RoundingMode roundingMode);

	RealBound pow(RealBound other, RoundingMode roundingMode);

	int signum();

	int compareBound(RealBound other);

	IntBound asInt();

	enum Infinite implements RealBound {
		POSITIVE_INFINITY {
			@Override
			public boolean lessThanOrEquals(RealBound other) {
				return this == other;
			}

			@Override
			public RealBound minus(RoundingMode roundingMode) {
				return NEGATIVE_INFINITY;
			}

			@Override
			public RealBound add(RealBound other, RoundingMode roundingMode) {
				return other == NEGATIVE_INFINITY ? roundingMode.error() : POSITIVE_INFINITY;
			}

			@Override
			public RealBound sub(RealBound other, RoundingMode roundingMode) {
				return other == POSITIVE_INFINITY ? roundingMode.error() : POSITIVE_INFINITY;
			}

			@Override
			public RealBound mul(RealBound other, RoundingMode roundingMode) {
				var signum = other.signum();
				if (signum == 0) {
					return Finite.ZERO;
				}
				return signum < 0 ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
			}

			public RealBound div(RealBound other, RoundingMode roundingMode) {
				var signum = other.signum();
				if (signum == 0) {
					return roundingMode.infinity();
				}
				return signum < 0 ? NEGATIVE_INFINITY : POSITIVE_INFINITY;
			}

			@Override
			public RealBound exp(RoundingMode roundingMode) {
				return this;
			}

			@Override
			public RealBound log(RoundingMode roundingMode) {
				return this;
			}

			@Override
			public RealBound sqrt(RoundingMode roundingMode) {
				return this;
			}

			@Override
			public RealBound pow(RealBound other, RoundingMode roundingMode) {
				int sign = other.signum();
				if (sign < 0) {
					return Finite.ZERO;
				}
				if (sign == 0) {
					return Finite.ONE;
				}
				return POSITIVE_INFINITY;
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
			public int compareBound(RealBound other) {
				return other == POSITIVE_INFINITY ? 0 : -1;
			}

			@Override
			public IntBound asInt() {
				return IntBound.Infinite.POSITIVE_INFINITY;
			}
		},

		NEGATIVE_INFINITY {
			@Override
			public boolean lessThanOrEquals(RealBound other) {
				return true;
			}

			@Override
			public RealBound minus(RoundingMode roundingMode) {
				return NEGATIVE_INFINITY;
			}

			@Override
			public RealBound add(RealBound other, RoundingMode roundingMode) {
				return other == POSITIVE_INFINITY ? roundingMode.error() : NEGATIVE_INFINITY;
			}

			@Override
			public RealBound sub(RealBound other, RoundingMode roundingMode) {
				return other == NEGATIVE_INFINITY ? roundingMode.error() : NEGATIVE_INFINITY;
			}

			@Override
			public RealBound mul(RealBound other, RoundingMode roundingMode) {
				var signum = other.signum();
				if (signum == 0) {
					return Finite.ZERO;
				}
				return signum < 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
			}

			public RealBound div(RealBound other, RoundingMode roundingMode) {
				var signum = other.signum();
				if (signum == 0) {
					return roundingMode.infinity();
				}
				return signum < 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY;
			}

			@Override
			public RealBound exp(RoundingMode roundingMode) {
				return Finite.ZERO;
			}

			@Override
			public RealBound log(RoundingMode roundingMode) {
				throw new ArithmeticException();
			}

			@Override
			public RealBound sqrt(RoundingMode roundingMode) {
				throw new ArithmeticException();
			}

			@Override
			public RealBound pow(RealBound other, RoundingMode roundingMode) {
				throw new ArithmeticException();
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
			public int compareBound(RealBound other) {
				return other == NEGATIVE_INFINITY ? 0 : 1;
			}

			@Override
			public IntBound asInt() {
				return IntBound.Infinite.NEGATIVE_INFINITY;
			}
		};

		@Override
		public boolean isFinite() {
			return false;
		}

		@Override
		public RealBound round(RoundingMode roundingMode) {
			return this;
		}
	}

	record Finite(BigDecimal value) implements RealBound {
		public static final Finite ZERO = new Finite(BigDecimal.ZERO);
		public static final Finite ONE = new Finite(BigDecimal.ONE);
		public static final Finite NEGATIVE_ONE = new Finite(BigDecimal.valueOf(-1));

		@Override
		public boolean lessThanOrEquals(RealBound other) {
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
		public RealBound round(RoundingMode roundingMode) {
			var context = roundingMode.context();
			if (value.precision() <= context.getPrecision()) {
				return this;
			}
			return new Finite(value.round(context));
		}

		@Override
		public RealBound minus(RoundingMode roundingMode) {
			return new Finite(value.multiply(NEGATIVE_ONE.value, roundingMode.context()));
		}

		@Override
		public RealBound add(RealBound other, RoundingMode roundingMode) {
			return switch (other) {
				case Infinite.POSITIVE_INFINITY -> Infinite.POSITIVE_INFINITY;
				case Infinite.NEGATIVE_INFINITY -> Infinite.NEGATIVE_INFINITY;
				case Finite(var otherValue) -> new Finite(value.add(otherValue, roundingMode.context()));
			};
		}

		@Override
		public RealBound sub(RealBound other, RoundingMode roundingMode) {
			return switch (other) {
				case Infinite.POSITIVE_INFINITY -> Infinite.NEGATIVE_INFINITY;
				case Infinite.NEGATIVE_INFINITY -> Infinite.POSITIVE_INFINITY;
				case Finite(var otherValue) -> new Finite(value.subtract(otherValue, roundingMode.context()));
			};
		}

		@Override
		public RealBound mul(RealBound other, RoundingMode roundingMode) {
			return switch (other) {
				case Infinite ignored -> other.mul(this, roundingMode);
				case Finite(var otherValue) -> new Finite(value.multiply(otherValue, roundingMode.context()));
			};
		}

		@Override
		public RealBound div(RealBound other, RoundingMode roundingMode) {
			return switch (other) {
				case Infinite ignored -> ZERO;
				case Finite(var otherValue) -> otherValue.compareTo(BigDecimal.ZERO) == 0 ?
						roundingMode.infinity() : new Finite(value.divide(otherValue, roundingMode.context()));
			};
		}

		@Override
		public RealBound exp(RoundingMode roundingMode) {
			return new Finite(BigDecimalMath.exp(value, roundingMode.context()));
		}

		@Override
		public RealBound log(RoundingMode roundingMode) {
			int compare = value.compareTo(BigDecimal.ZERO);
			if (compare < 0) {
				throw new ArithmeticException();
			}
			if (compare == 0) {
				return Infinite.NEGATIVE_INFINITY;
			}
			return new Finite(BigDecimalMath.log(value, roundingMode.context()));
		}

		@Override
		public RealBound sqrt(RoundingMode roundingMode) {
			int compare = value.compareTo(BigDecimal.ZERO);
			if (compare < 0) {
				throw new ArithmeticException();
			}
			return new Finite(BigDecimalMath.sqrt(value, roundingMode.context()));
		}

		@Override
		public RealBound pow(RealBound other, RoundingMode roundingMode) {
			int signum = signum();
			if (signum < 0) {
				throw new ArithmeticException();
			}
			if (signum == 0) {
				// To preserve (anti-)monotonicity, negative powers of 0 are considered as 1.
				// The case of raising 0 to a surely negative power is handled in
				// {@link RealInterval#pow(RealInterval)} instead, so in the current method, a value of 0 stands for
				// a very small (but positive) number instead.
				int otherSignum = other.signum();
				if (otherSignum < 0) {
					return Infinite.POSITIVE_INFINITY;
				}
				if (otherSignum == 0) {
					return ONE;
				}
				return ZERO;
			}
			return switch (other) {
				case Infinite ignored -> {
					int compareTo1 = value.compareTo(BigDecimal.ONE);
					if (compareTo1 < 0) {
						yield other == Infinite.POSITIVE_INFINITY ? ZERO : Infinite.POSITIVE_INFINITY;
					}
					if (compareTo1 == 0) {
						yield ONE;
					}
					yield other == Infinite.POSITIVE_INFINITY ? Infinite.POSITIVE_INFINITY : ZERO;
				}
				case Finite(var finiteValue) ->
						new Finite(BigDecimalMath.pow(value, finiteValue, roundingMode.context()));
			};
		}

		@Override
		public int signum() {
			return value.signum();
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Finite(var otherValue))) {
				return false;
			}
			return value.compareTo(otherValue) == 0;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(value);
		}

		@Override
		public @NotNull String toString() {
			var string = value.toString();
			// Make sure the number is parsed as a real number literal by the Refinery language grammar.
			return string.indexOf('.') < 0 && string.indexOf('E') < 0 ? string + ".0" : string;
		}

		@Override
		public int compareBound(RealBound other) {
			return other instanceof Finite(var otherValue) ? value.compareTo(otherValue) :
					-other.compareBound(this);
		}

		@Override
		public IntBound asInt() {
			return IntBound.of(value);
		}
	}

	static RealBound of(BigDecimal value) {
		return new RealBound.Finite(value);
	}

	static RealBound fromInt(IntBound intValue, RoundingMode roundingMode) {
		return switch (intValue) {
			case IntBound.Infinite.POSITIVE_INFINITY -> Infinite.POSITIVE_INFINITY;
			case IntBound.Infinite.NEGATIVE_INFINITY -> Infinite.NEGATIVE_INFINITY;
			case IntBound.Finite finiteBound ->
					new Finite(new BigDecimal(finiteBound.value()).round(roundingMode.context()));
		};
	}
}
