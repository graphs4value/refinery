package tools.refinery.logic.term.intinterval;

public sealed interface Bound {
	boolean lessThanOrEquals(Bound other);

	default boolean greaterThanOrEquals(Bound other) {
		return other.lessThanOrEquals(this);
	}

	boolean equals(Bound other);

	boolean isFinite();

	default Bound min(Bound other) {
		return lessThanOrEquals(other) ? this : other;
	}

	default Bound max(Bound other) {
		return greaterThanOrEquals(other) ? this : other;
	}

	enum Infinite implements Bound {
		POSITIVE_INFINITY {
			@Override
			public boolean lessThanOrEquals(Bound other) {
				return this == other;
			}

			@Override
			public boolean equals(Bound other) {
				return this == other;
			}

			@Override
			public String toString() {
				return "∞";
			}
		},
		NEGATIVE_INFINITY {
			@Override
			public boolean lessThanOrEquals(Bound other) {
				return true;
			}

			@Override
			public boolean equals(Bound other) {
				return this == other;
			}

			@Override
			public String toString() {
				return "-∞";
			}
		};

		@Override
		public boolean isFinite() {
			return false;
		}
	}

	record Finite(int value) implements Bound {
		@Override
		public boolean lessThanOrEquals(Bound other) {
			return switch (other) {
				case Infinite.POSITIVE_INFINITY -> true;
				case Infinite.NEGATIVE_INFINITY -> false;
				case Finite(int otherValue) -> value <= otherValue;
			};
		}

		@Override
		public boolean equals(Bound other) {
			return switch (other) {
				case Infinite.POSITIVE_INFINITY, Infinite.NEGATIVE_INFINITY -> false;
				case Finite(int otherValue) -> value == otherValue;
			};
		}

		@Override
		public boolean isFinite() {
			return true;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}
	}

	static Bound of(int value) {
		return new Finite(value);
	}
}
