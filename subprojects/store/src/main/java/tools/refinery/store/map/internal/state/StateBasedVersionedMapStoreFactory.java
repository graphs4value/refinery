/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.state;

import tools.refinery.store.map.*;

import java.util.List;

public class StateBasedVersionedMapStoreFactory<K, V> implements VersionedMapStoreFactory<K, V> {
	private final V defaultValue;
	private final ContinuousHashProvider<K> continuousHashProvider;
	private final VersionedMapStoreStateConfiguration config;

	public StateBasedVersionedMapStoreFactory(V defaultValue, Boolean transformToImmutable,
											  VersionedMapStoreFactoryBuilder.SharingStrategy sharingStrategy,
											  boolean versionFreeingEnabled,
											  ContinuousHashProvider<K> continuousHashProvider) {
		this.defaultValue = defaultValue;
		this.continuousHashProvider = continuousHashProvider;

		this.config = new VersionedMapStoreStateConfiguration(
				transformToImmutable,
				sharingStrategy == VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE
						|| sharingStrategy == VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE_IN_GROUP,
				sharingStrategy == VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE_IN_GROUP,
				versionFreeingEnabled);
	}

	@Override
	public VersionedMapStore<K, V> createOne() {
		return new VersionedMapStoreStateImpl<>(continuousHashProvider, defaultValue, config);

	}

	@Override
	public List<VersionedMapStore<K, V>> createGroup(int amount) {
		return VersionedMapStoreStateImpl.createSharedVersionedMapStores(amount, continuousHashProvider, defaultValue,
				config);
	}
}
