package tools.refinery.store.model.representation.cardinality;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FiniteUpperCardinalityTest {
	@Test
	void invalidConstructorTest() {
		assertThrows(IllegalArgumentException.class, () -> new FiniteUpperCardinality(-1));
	}
}
