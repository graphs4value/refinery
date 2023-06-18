/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation.cardinality;

import org.junit.jupiter.api.Test;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.representation.cardinality.UpperCardinalities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class CardinalityIntervalsTest {
	@Test
	void betweenEmptyTest() {
		var interval = CardinalityIntervals.between(2, 1);
		assertThat(interval.isEmpty(), equalTo(true));
	}

	@Test
	void betweenNegativeUpperBoundTest() {
		var interval = CardinalityIntervals.between(0, -1);
		assertThat(interval.upperBound(), equalTo(UpperCardinalities.UNBOUNDED));
		assertThat(interval.isEmpty(), equalTo(false));
	}
}
