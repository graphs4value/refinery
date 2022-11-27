package tools.refinery.store.model.representation.cardinality;

import org.junit.jupiter.api.Test;

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
