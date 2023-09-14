/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation.cardinality;

import org.jetbrains.annotations.Nullable;

public sealed interface UpperCardinality extends Comparable<UpperCardinality> permits FiniteUpperCardinality,
		UnboundedUpperCardinality {
	default UpperCardinality min(UpperCardinality other) {
		return this.compareTo(other) <= 0 ? this : other;
	}

	default UpperCardinality max(UpperCardinality other) {
		return this.compareTo(other) >= 0 ? this : other;
	}

	UpperCardinality add(UpperCardinality other);

	@Nullable
	UpperCardinality take(int count);

	UpperCardinality multiply(UpperCardinality other);

	int compareToInt(int value);

	static UpperCardinality of(int upperBound) {
		return UpperCardinalities.atMost(upperBound);
	}
}
