/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreFactoryBuilder;
import tools.refinery.store.map.internal.state.InOrderMapCursor;
import tools.refinery.store.map.internal.state.VersionedMapStateImpl;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class InOrderCursorTest {
	@Test
	void testCursor() {
		var store = VersionedMapStore.<Integer,String>builder()
				.strategy(VersionedMapStoreFactoryBuilder.StoreStrategy.STATE)
				.stateBasedImmutableWhenCommitting(true)
				.stateBasedHashProvider(MapTestEnvironment.prepareHashProvider(false))
				.stateBasedSharingStrategy(VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE)
				.defaultValue("x")
				.build()
				.createOne();

		VersionedMapStateImpl<Integer,String> map = (VersionedMapStateImpl<Integer,String>) store.createMap();
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

	private void checkMove(VersionedMapStateImpl<Integer,String> map, int num) {
		InOrderMapCursor<Integer,String> cursor = new InOrderMapCursor<>(map);
		for(int i=0; i<num; i++) {
			assertTrue(cursor.move());
			assertFalse(cursor.isTerminated());
		}
		assertFalse(cursor.move());
		assertTrue(cursor.isTerminated());
	}
}
