/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

import java.util.List;

public interface VersionedMapStoreFactory<K,V> {
	/**
	 * Constructs a new instance of {@link VersionedMap}.
	 * @return The new instance.
	 */
	VersionedMapStore<K,V> createOne();

	/**
	 * Constructs a group of {@link VersionedMap}s with the same configuration. If possible, the stores share
	 * resources with each other.
	 * @param amount The amount of new instances to be created.
	 * @return A list of new stores with the given number of elements.
	 */
	List<VersionedMapStore<K, V>> createGroup(int amount);
}
