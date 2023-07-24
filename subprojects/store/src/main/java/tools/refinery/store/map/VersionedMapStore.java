/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

import tools.refinery.store.map.internal.VersionedMapStoreFactoryBuilderImpl;

import java.util.Set;

public interface VersionedMapStore<K, V> {

	VersionedMap<K, V> createMap();

	VersionedMap<K, V> createMap(long state);

	Set<Long> getStates();

	DiffCursor<K,V> getDiffCursor(long fromState, long toState);

	static <K,V> VersionedMapStoreFactoryBuilder<K,V> builder() {
		return new VersionedMapStoreFactoryBuilderImpl<>();
	}
}
