/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation.cardinality;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntBinaryOperator;

public record FiniteUpperCardinality(int finiteUpperBound) implements UpperCardinality {
	public FiniteUpperCardinality {
		if (finiteUpperBound < 0) {
			throw new IllegalArgumentException("finiteUpperBound must not be negative");
		}
	}

	@Override
	public UpperCardinality add(UpperCardinality other) {
		return lift(other, Integer::sum);
	}

	@Override
	@Nullable
	public UpperCardinality take(int count) {
		if (finiteUpperBound < count) {
			return null;
		}
		return new FiniteUpperCardinality(finiteUpperBound - count);
	}

	@Override
	public UpperCardinality multiply(UpperCardinality other) {
		return lift(other, (a, b) -> a * b);
	}

	@Override
	public int compareTo(@NotNull UpperCardinality upperCardinality) {
		if (upperCardinality instanceof FiniteUpperCardinality finiteUpperCardinality) {
			return compareToInt(finiteUpperCardinality.finiteUpperBound);
		}
		if (upperCardinality instanceof UnboundedUpperCardinality) {
			return -1;
		}
		throw new IllegalArgumentException("Unknown UpperCardinality: " + upperCardinality);
	}

	@Override
	public int compareToInt(int value) {
		return Integer.compare(finiteUpperBound, value);
	}

	@Override
	public String toString() {
		return Integer.toString(finiteUpperBound);
	}

	private UpperCardinality lift(@NotNull UpperCardinality other, IntBinaryOperator operator) {
		if (other instanceof FiniteUpperCardinality finiteUpperCardinality) {
			return UpperCardinalities.atMost(operator.applyAsInt(finiteUpperBound,
					finiteUpperCardinality.finiteUpperBound));
		}
		if (other instanceof UnboundedUpperCardinality) {
			return UpperCardinalities.UNBOUNDED;
		}
		throw new IllegalArgumentException("Unknown UpperCardinality: " + other);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FiniteUpperCardinality that = (FiniteUpperCardinality) o;
		return finiteUpperBound == that.finiteUpperBound;
	}

	@Override
	public int hashCode() {
		return finiteUpperBound;
	}
}
