package tools.refinery.store.map.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreImpl;
import tools.refinery.store.model.TupleHashProvider;
import tools.refinery.store.tuple.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapUnitTests {
	@Test
	void defaultTest() {
		VersionedMapStore<Tuple, Boolean> store = new VersionedMapStoreImpl<>(TupleHashProvider.INSTANCE, false);
		var map = store.createMap();
		var out1 = map.put(Tuple.of(0), true);
		assertEquals(false, out1);
		var out2 = map.put(Tuple.of(1), true);
		assertEquals(false, out2);
	}
}
