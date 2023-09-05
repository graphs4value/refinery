/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.internal.state.VersionedMapStoreStateImpl;
import tools.refinery.store.model.TupleHashProvider;
import tools.refinery.store.tuple.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapUnitTests {
	@Test
	void defaultTest() {
		VersionedMapStore<Tuple, Boolean> store = new VersionedMapStoreStateImpl<>(TupleHashProvider.INSTANCE, false);
		var map = store.createMap();
		var out1 = map.put(Tuple.of(0), true);
		assertEquals(false, out1);
		var out2 = map.put(Tuple.of(1), true);
		assertEquals(false, out2);
	}

	@Test
	void deltaRestoreTest() {
		VersionedMapStore<Integer,String> store =
				VersionedMapStore.<Integer,String>builder().defaultValue("x").build().createOne();
		var map = store.createMap();
		map.put(1,"val");
		var version1 = map.commit();
		map.put(1,"x");
		map.restore(version1);
		System.out.println(map.getSize());
		assertEquals(1,map.getSize());
	}

	@Test
	void deltaRestoreTest2() {
		VersionedMapStore<Integer,String> store =
				VersionedMapStore.<Integer,String>builder().defaultValue("x").build().createOne();
		var map = store.createMap();
		map.put(1,"x");
		var version1 = map.commit();
		map.put(1,"1");
		map.restore(version1);
		System.out.println(map.getSize());
		assertEquals(0,map.getSize());
	}
	@Test
	void deltaRestoreTest3() {
		VersionedMapStore<Integer,String> store =
				VersionedMapStore.<Integer,String>builder().defaultValue("x").build().createOne();
		var map = store.createMap();
		map.commit();
		map.put(1,"1");
		map.put(2,"x");
		assertEquals(1,map.getSize());
		var version1 = map.commit();
		map.put(1,"x");
		assertEquals(0,map.getSize());
		map.put(2,"2");
		assertEquals(1,map.getSize());
		map.put(2,"x");
		assertEquals(0,map.getSize());
		var version2 = map.commit();
		map.restore(version1);
		assertEquals(1,map.getSize());
		map.restore(version2);
		assertEquals(0,map.getSize());
	}

	@Test
	void deltaRestoreTest4() {
		VersionedMapStore<Integer,String> store =
				VersionedMapStore.<Integer,String>builder().defaultValue("x").build().createOne();
		var map = store.createMap();
		map.commit();
		map.put(1,"1");
		map.put(2,"x");
		assertEquals(1,map.getSize());
		var version1 = map.commit();
		map.put(1,"x");
		assertEquals(0,map.getSize());
		map.put(2,"2");
		assertEquals(1,map.getSize());
		map.put(2,"x");
		assertEquals(0,map.getSize());
		var version2 = map.commit();
		map.restore(version1);
		assertEquals(1,map.getSize());
		map.restore(version2);
		assertEquals(0,map.getSize());
	}
}
