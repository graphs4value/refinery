/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.delta;

import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreFactory;
import tools.refinery.store.map.VersionedMapStoreFactoryBuilder;

import java.util.ArrayList;
import java.util.List;

public class DeltaBasedVersionedMapStoreFactory<K, V> implements VersionedMapStoreFactory<K, V> {
	private final V defaultValue;
	private final boolean summarizeChanges;

	public DeltaBasedVersionedMapStoreFactory(V defaultValue,
											  VersionedMapStoreFactoryBuilder.DeltaTransactionStrategy deltaTransactionStrategy) {
		this.defaultValue = defaultValue;
		this.summarizeChanges = deltaTransactionStrategy == VersionedMapStoreFactoryBuilder.DeltaTransactionStrategy.SET;
	}

	@Override
	public VersionedMapStore<K, V> createOne() {
		return new VersionedMapStoreDeltaImpl<>(summarizeChanges, defaultValue);
	}

	@Override
	public List<VersionedMapStore<K, V>> createGroup(int amount) {
		List<VersionedMapStore<K, V>> result = new ArrayList<>(amount);
		for(int i=0; i<amount; i++) {
			result.add(new VersionedMapStoreDeltaImpl<>(summarizeChanges,defaultValue));
		}
		return result;
	}
}
