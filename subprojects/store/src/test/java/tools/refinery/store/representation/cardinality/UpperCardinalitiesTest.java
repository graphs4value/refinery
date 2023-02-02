package tools.refinery.store.representation.cardinality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.store.representation.cardinality.FiniteUpperCardinality;
import tools.refinery.store.representation.cardinality.UnboundedUpperCardinality;
import tools.refinery.store.representation.cardinality.UpperCardinalities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

class UpperCardinalitiesTest {
	@ParameterizedTest
	@ValueSource(ints = {0, 1, 255, 256, 1000, Integer.MAX_VALUE})
	void valueOfBoundedTest(int value) {
		var upperCardinality = UpperCardinalities.valueOf(value);
		assertThat(upperCardinality, instanceOf(FiniteUpperCardinality.class));
		assertThat(((FiniteUpperCardinality) upperCardinality).finiteUpperBound(), equalTo(value));
	}

	@Test
	void valueOfUnboundedTest() {
		var upperCardinality = UpperCardinalities.valueOf(-1);
		assertThat(upperCardinality, instanceOf(UnboundedUpperCardinality.class));
	}
}