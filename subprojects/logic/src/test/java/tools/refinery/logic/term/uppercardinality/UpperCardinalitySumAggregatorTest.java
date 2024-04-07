/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.uppercardinality;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.refinery.logic.term.StatefulAggregate;

import static org.hamcrest.MatcherAssert.assertThat;

class UpperCardinalitySumAggregatorTest {
	private StatefulAggregate<UpperCardinality, UpperCardinality> accumulator;

	@BeforeEach
	void beforeEach() {
		accumulator = UpperCardinalitySumAggregator.INSTANCE.createEmptyAggregate();
	}

	@Test
	void emptyAggregationTest() {
		MatcherAssert.assertThat(accumulator.getResult(), Matchers.is(UpperCardinality.of(0)));
	}

	@Test
	void singleBoundedTest() {
		accumulator.add(UpperCardinality.of(3));
		MatcherAssert.assertThat(accumulator.getResult(), Matchers.is(UpperCardinality.of(3)));
	}

	@Test
	void multipleBoundedTest() {
		accumulator.add(UpperCardinality.of(2));
		accumulator.add(UpperCardinality.of(3));
		MatcherAssert.assertThat(accumulator.getResult(), Matchers.is(UpperCardinality.of(5)));
	}

	@Test
	void singleUnboundedTest() {
		accumulator.add(UpperCardinalities.UNBOUNDED);
		assertThat(accumulator.getResult(), Matchers.is(UpperCardinalities.UNBOUNDED));
	}

	@Test
	void multipleUnboundedTest() {
		accumulator.add(UpperCardinalities.UNBOUNDED);
		accumulator.add(UpperCardinalities.UNBOUNDED);
		assertThat(accumulator.getResult(), Matchers.is(UpperCardinalities.UNBOUNDED));
	}

	@Test
	void removeBoundedTest() {
		accumulator.add(UpperCardinality.of(2));
		accumulator.add(UpperCardinality.of(3));
		accumulator.remove(UpperCardinality.of(2));
		MatcherAssert.assertThat(accumulator.getResult(), Matchers.is(UpperCardinality.of(3)));
	}

	@Test
	void removeAllUnboundedTest() {
		accumulator.add(UpperCardinalities.UNBOUNDED);
		accumulator.add(UpperCardinality.of(3));
		accumulator.remove(UpperCardinalities.UNBOUNDED);
		MatcherAssert.assertThat(accumulator.getResult(), Matchers.is(UpperCardinality.of(3)));
	}

	@Test
	void removeSomeUnboundedTest() {
		accumulator.add(UpperCardinalities.UNBOUNDED);
		accumulator.add(UpperCardinalities.UNBOUNDED);
		accumulator.add(UpperCardinality.of(3));
		accumulator.remove(UpperCardinalities.UNBOUNDED);
		assertThat(accumulator.getResult(), Matchers.is(UpperCardinalities.UNBOUNDED));
	}
}
