package tools.refinery.store.representation.cardinality;

import org.junit.jupiter.api.Test;
import tools.refinery.store.representation.cardinality.FiniteUpperCardinality;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FiniteUpperCardinalityTest {
	@Test
	void invalidConstructorTest() {
		assertThrows(IllegalArgumentException.class, () -> new FiniteUpperCardinality(-1));
	}
}
