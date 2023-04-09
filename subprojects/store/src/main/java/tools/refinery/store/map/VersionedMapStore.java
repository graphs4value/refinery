/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

import java.util.Set;

public interface VersionedMapStore<K, V> {
	
	public VersionedMap<K, V> createMap();

	public VersionedMap<K, V> createMap(long state);
	
	public Set<Long> getStates();

	public DiffCursor<K,V> getDiffCursor(long fromState, long toState);
}