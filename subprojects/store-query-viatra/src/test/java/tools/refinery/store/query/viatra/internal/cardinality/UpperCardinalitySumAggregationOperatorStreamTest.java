package tools.refinery.store.query.viatra.internal.cardinality;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.store.representation.cardinality.UpperCardinalities;
import tools.refinery.store.representation.cardinality.UpperCardinality;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class UpperCardinalitySumAggregationOperatorStreamTest {
	@ParameterizedTest
	@MethodSource
	void testStream(Stream<UpperCardinality> stream, UpperCardinality expected) {
		var result = UpperCardinalitySumAggregationOperator.INSTANCE.aggregateStream(stream);
		assertThat(result, equalTo(expected));
	}

	static Stream<Arguments> testStream() {
		return Stream.of(
				Arguments.of(Stream.of(), UpperCardinalities.ZERO),
				Arguments.of(Stream.of(UpperCardinality.of(3)), UpperCardinality.of(3)),
				Arguments.of(
						Stream.of(
								UpperCardinality.of(2),
								UpperCardinality.of(3)
						),
						UpperCardinality.of(5)
				),
				Arguments.of(Stream.of(UpperCardinalities.UNBOUNDED), UpperCardinalities.UNBOUNDED),
				Arguments.of(
						Stream.of(
								UpperCardinalities.UNBOUNDED,
								UpperCardinalities.UNBOUNDED
						),
						UpperCardinalities.UNBOUNDED
				),
				Arguments.of(
						Stream.of(
								UpperCardinalities.UNBOUNDED,
								UpperCardinality.of(3)
						),
						UpperCardinalities.UNBOUNDED
				)
		);
	}
}
