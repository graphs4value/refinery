package tools.refinery.store.representation.cardinality;

import org.junit.jupiter.api.Test;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

class EmptyCardinalityIntervalTest {
	@Test
	void inconsistentBoundsTest() {
		assertThat(CardinalityIntervals.ERROR.upperBound().compareToInt(CardinalityIntervals.ERROR.lowerBound()),
				lessThan(0));
	}
}
