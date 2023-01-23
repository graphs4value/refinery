package tools.refinery.store.query.viatra.internal.cardinality;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.refinery.store.representation.cardinality.UpperCardinalities;
import tools.refinery.store.representation.cardinality.UpperCardinality;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class UpperCardinalitySumAggregationOperatorTest {
	private UpperCardinalitySumAggregationOperator.Accumulator accumulator;

	@BeforeEach
	void beforeEach() {
		accumulator = UpperCardinalitySumAggregationOperator.INSTANCE.createNeutral();
	}

	@Test
	void emptyAggregationTest() {
		assertResult(UpperCardinality.of(0));
	}

	@Test
	void singleBoundedTest() {
		insert(UpperCardinality.of(3));
		assertResult(UpperCardinality.of(3));
	}

	@Test
	void multipleBoundedTest() {
		insert(UpperCardinality.of(2));
		insert(UpperCardinality.of(3));
		assertResult(UpperCardinality.of(5));
	}

	@Test
	void singleUnboundedTest() {
		insert(UpperCardinalities.UNBOUNDED);
		assertResult(UpperCardinalities.UNBOUNDED);
	}

	@Test
	void multipleUnboundedTest() {
		insert(UpperCardinalities.UNBOUNDED);
		insert(UpperCardinalities.UNBOUNDED);
		assertResult(UpperCardinalities.UNBOUNDED);
	}

	@Test
	void removeBoundedTest() {
		insert(UpperCardinality.of(2));
		insert(UpperCardinality.of(3));
		remove(UpperCardinality.of(2));
		assertResult(UpperCardinality.of(3));
	}

	@Test
	void removeAllUnboundedTest() {
		insert(UpperCardinalities.UNBOUNDED);
		insert(UpperCardinality.of(3));
		remove(UpperCardinalities.UNBOUNDED);
		assertResult(UpperCardinality.of(3));
	}

	@Test
	void removeSomeUnboundedTest() {
		insert(UpperCardinalities.UNBOUNDED);
		insert(UpperCardinalities.UNBOUNDED);
		insert(UpperCardinality.of(3));
		remove(UpperCardinalities.UNBOUNDED);
		assertResult(UpperCardinalities.UNBOUNDED);
	}

	private void insert(UpperCardinality value) {
		accumulator = UpperCardinalitySumAggregationOperator.INSTANCE.update(accumulator, value, true);
	}

	private void remove(UpperCardinality value) {
		accumulator = UpperCardinalitySumAggregationOperator.INSTANCE.update(accumulator, value, false);
	}

	private void assertResult(UpperCardinality expected) {
		var result = UpperCardinalitySumAggregationOperator.INSTANCE.getAggregate(accumulator);
		assertThat(result, equalTo(expected));
	}
}
