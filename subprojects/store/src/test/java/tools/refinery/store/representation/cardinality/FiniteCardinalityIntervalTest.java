/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation.cardinality;

import org.junit.jupiter.api.Test;
import tools.refinery.store.representation.cardinality.NonEmptyCardinalityInterval;
import tools.refinery.store.representation.cardinality.UpperCardinalities;
import tools.refinery.store.representation.cardinality.UpperCardinality;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FiniteCardinalityIntervalTest {
	@Test
	void invalidLowerBoundConstructorTest() {
		assertThrows(IllegalArgumentException.class, () -> new NonEmptyCardinalityInterval(-1,
				UpperCardinalities.UNBOUNDED));
	}

	@Test
	void invalidUpperBoundConstructorTest() {
		var upperCardinality = UpperCardinality.of(1);
		assertThrows(IllegalArgumentException.class, () -> new NonEmptyCardinalityInterval(2,
				upperCardinality));
	}
}
