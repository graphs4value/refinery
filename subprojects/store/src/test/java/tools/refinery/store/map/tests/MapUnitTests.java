package tools.refinery.store.map.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreImpl;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.model.TupleHashProvider;

class MapUnitTests {
	@Test
	void defaultTest() {
		VersionedMapStore<Tuple, Boolean> store = new VersionedMapStoreImpl<Tuple, Boolean>(TupleHashProvider.singleton(), false);
		var map = store.createMap();
		var out1 = map.put(Tuple.of(0), true);
		assertEquals(false, out1);
		var out2 = map.put(Tuple.of(1), true);
		assertEquals(false, out2);
	}
}
