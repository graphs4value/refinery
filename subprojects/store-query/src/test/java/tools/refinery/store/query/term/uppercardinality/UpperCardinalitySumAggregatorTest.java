/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.uppercardinality;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.refinery.store.query.term.StatefulAggregate;
import tools.refinery.store.representation.cardinality.UpperCardinalities;
import tools.refinery.store.representation.cardinality.UpperCardinality;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class UpperCardinalitySumAggregatorTest {
	private StatefulAggregate<UpperCardinality, UpperCardinality> accumulator;

	@BeforeEach
	void beforeEach() {
		accumulator = UpperCardinalitySumAggregator.INSTANCE.createEmptyAggregate();
	}

	@Test
	void emptyAggregationTest() {
		assertThat(accumulator.getResult(), is(UpperCardinality.of(0)));
	}

	@Test
	void singleBoundedTest() {
		accumulator.add(UpperCardinality.of(3));
		assertThat(accumulator.getResult(), is(UpperCardinality.of(3)));
	}

	@Test
	void multipleBoundedTest() {
		accumulator.add(UpperCardinality.of(2));
		accumulator.add(UpperCardinality.of(3));
		assertThat(accumulator.getResult(), is(UpperCardinality.of(5)));
	}

	@Test
	void singleUnboundedTest() {
		accumulator.add(UpperCardinalities.UNBOUNDED);
		assertThat(accumulator.getResult(), is(UpperCardinalities.UNBOUNDED));
	}

	@Test
	void multipleUnboundedTest() {
		accumulator.add(UpperCardinalities.UNBOUNDED);
		accumulator.add(UpperCardinalities.UNBOUNDED);
		assertThat(accumulator.getResult(), is(UpperCardinalities.UNBOUNDED));
	}

	@Test
	void removeBoundedTest() {
		accumulator.add(UpperCardinality.of(2));
		accumulator.add(UpperCardinality.of(3));
		accumulator.remove(UpperCardinality.of(2));
		assertThat(accumulator.getResult(), is(UpperCardinality.of(3)));
	}

	@Test
	void removeAllUnboundedTest() {
		accumulator.add(UpperCardinalities.UNBOUNDED);
		accumulator.add(UpperCardinality.of(3));
		accumulator.remove(UpperCardinalities.UNBOUNDED);
		assertThat(accumulator.getResult(), is(UpperCardinality.of(3)));
	}

	@Test
	void removeSomeUnboundedTest() {
		accumulator.add(UpperCardinalities.UNBOUNDED);
		accumulator.add(UpperCardinalities.UNBOUNDED);
		accumulator.add(UpperCardinality.of(3));
		accumulator.remove(UpperCardinalities.UNBOUNDED);
		assertThat(accumulator.getResult(), is(UpperCardinalities.UNBOUNDED));
	}
}
