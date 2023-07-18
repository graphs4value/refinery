package tools.refinery.store.map.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.map.VersionedMapStoreBuilder;
import tools.refinery.store.map.internal.InOrderMapCursor;
import tools.refinery.store.map.internal.VersionedMapImpl;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class InOrderCursorTest {
	@Test
	void testCursor() {
		var store = VersionedMapStoreBuilder.<Integer,String>builder()
				.setStrategy(VersionedMapStoreBuilder.StoreStrategy.STATE)
				.setStateBasedImmutableWhenCommitting(true)
				.setHashProvider(MapTestEnvironment.prepareHashProvider(false))
				.setStateBasedNodeSharingStrategy(VersionedMapStoreBuilder.StateStorageStrategy.SHARED_NODE_CACHE)
				.setDefaultValue("x")
				.buildOne();

		VersionedMapImpl<Integer,String> map = (VersionedMapImpl<Integer,String>) store.createMap();
		checkMove(map,0);

		map.put(1,"A");
		map.commit();
		checkMove(map,1);


		map.put(2,"B");
		map.commit();
		checkMove(map,2);

		map.put(3,"C");
		map.commit();
		checkMove(map,3);

	}

	private void checkMove(VersionedMapImpl<Integer,String> map, int num) {
		InOrderMapCursor<Integer,String> cursor = new InOrderMapCursor<>(map);
		for(int i=0; i<num; i++) {
			assertTrue(cursor.move());
			assertFalse(cursor.isTerminated());
		}
		assertFalse(cursor.move());
		assertTrue(cursor.isTerminated());
	}
}
