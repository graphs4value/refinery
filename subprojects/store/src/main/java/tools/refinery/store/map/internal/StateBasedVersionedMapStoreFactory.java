package tools.refinery.store.map.internal;

import tools.refinery.store.map.*;

import java.util.List;

public class StateBasedVersionedMapStoreFactory<K, V> implements VersionedMapStoreFactory<K, V> {
	private final V defaultValue;
	private final ContinousHashProvider<K> continousHashProvider;
	private final VersionedMapStoreConfiguration config;

	public StateBasedVersionedMapStoreFactory(V defaultValue, Boolean transformToImmutable, VersionedMapStoreFactoryBuilder.SharingStrategy sharingStrategy, ContinousHashProvider<K> continousHashProvider) {
		this.defaultValue = defaultValue;
		this.continousHashProvider = continousHashProvider;

		this.config = new VersionedMapStoreConfiguration(
				transformToImmutable,
				sharingStrategy == VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE || sharingStrategy == VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE_IN_GROUP,
				sharingStrategy == VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE_IN_GROUP);
	}

	@Override
	public VersionedMapStore<K, V> createOne() {
		return new VersionedMapStoreImpl<>(continousHashProvider, defaultValue, config);

	}

	@Override
	public List<VersionedMapStore<K, V>> createGroup(int amount) {
		return VersionedMapStoreImpl.createSharedVersionedMapStores(amount, continousHashProvider, defaultValue,
				config);
	}
}
