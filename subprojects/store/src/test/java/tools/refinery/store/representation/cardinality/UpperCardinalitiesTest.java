/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation.cardinality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

class UpperCardinalitiesTest {
	@ParameterizedTest
	@ValueSource(ints = {0, 1, 255, 256, 1000, Integer.MAX_VALUE})
	void valueOfBoundedTest(int value) {
		var upperCardinality = UpperCardinalities.atMost(value);
		assertThat(upperCardinality, instanceOf(FiniteUpperCardinality.class));
		assertThat(((FiniteUpperCardinality) upperCardinality).finiteUpperBound(), equalTo(value));
	}

	@Test
	void valueOfUnboundedTest() {
		var upperCardinality = UpperCardinalities.atMost(-1);
		assertThat(upperCardinality, instanceOf(UnboundedUpperCardinality.class));
	}
}
