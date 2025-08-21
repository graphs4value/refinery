/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.realinterval;

import org.jetbrains.annotations.NotNull;
import tools.refinery.logic.term.intinterval.IntBound;

import java.math.BigDecimal;

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

	RealBound minus(RoundingMode roundingMode);

	RealBound add(RealBound other, RoundingMode roundingMode);

	RealBound sub(RealBound other, RoundingMode roundingMode);

	RealBound mul(RealBound other, RoundingMode roundingMode);

	RealBound div(RealBound other, RoundingMode roundingMode);

	int signum();

	int compareBound(RealBound other);

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
			public int compareBound(RealBound other) {
				return other == POSITIVE_INFINITY ? 0 : -1;
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
			public int compareBound(RealBound other) {
				return other == NEGATIVE_INFINITY ? 0 : 1;
			}
		};

		@Override
		public boolean isFinite() {
			return false;
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
		public int signum() {
			return value.signum();
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
	}

	static RealBound of(BigDecimal value) {
		return new RealBound.Finite(value);
	}

	static RealBound fromInteger(IntBound intValue) {
		return switch (intValue) {
			case IntBound.Infinite.POSITIVE_INFINITY -> Infinite.POSITIVE_INFINITY;
			case IntBound.Infinite.NEGATIVE_INFINITY -> Infinite.NEGATIVE_INFINITY;
			case IntBound.Finite finiteBound ->
					new Finite(BigDecimal.valueOf(finiteBound.value()));
		};
	}
}
