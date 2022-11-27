package tools.refinery.store.model.representation.cardinality;

import org.junit.jupiter.api.Test;

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
