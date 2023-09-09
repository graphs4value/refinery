/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation.cardinality;

import org.jetbrains.annotations.NotNull;

// Singleton implementation, because there is only a single countable infinity.
@SuppressWarnings("squid:S6548")
public final class UnboundedUpperCardinality implements UpperCardinality {
	static final UnboundedUpperCardinality INSTANCE = new UnboundedUpperCardinality();

	private UnboundedUpperCardinality() {
		// Singleton constructor.
	}

	@Override
	public UpperCardinality add(UpperCardinality other) {
		return this;
	}

	@Override
	public UpperCardinality take(int count) {
		return this;
	}

	@Override
	public UpperCardinality multiply(UpperCardinality other) {
		return this;
	}

	// This should always be greater than any finite cardinality.
	@SuppressWarnings("ComparatorMethodParameterNotUsed")
	@Override
	public int compareTo(@NotNull UpperCardinality upperCardinality) {
		if (upperCardinality instanceof FiniteUpperCardinality) {
			return 1;
		}
		if (upperCardinality instanceof UnboundedUpperCardinality) {
			return 0;
		}
		throw new IllegalArgumentException("Unknown UpperCardinality: " + upperCardinality);
	}

	@Override
	public int compareToInt(int value) {
		return 1;
	}

	@Override
	public String toString() {
		return "*";
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || (obj != null && getClass() == obj.getClass());
	}

	@Override
	public int hashCode() {
		return -1;
	}
}
