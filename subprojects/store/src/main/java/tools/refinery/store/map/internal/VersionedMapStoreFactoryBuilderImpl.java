package tools.refinery.store.map.internal;

import tools.refinery.store.map.ContinousHashProvider;
import tools.refinery.store.map.VersionedMapStoreFactory;
import tools.refinery.store.map.VersionedMapStoreFactoryBuilder;

public class VersionedMapStoreFactoryBuilderImpl<K, V> implements VersionedMapStoreFactoryBuilder<K, V> {

	private boolean defaultSet = false;
	private V defaultValue;
	private StoreStrategy strategy = null;
	private Boolean transformToImmutable = null;
	private SharingStrategy sharingStrategy = null;
	private ContinousHashProvider<K> continousHashProvider = null;
	private DeltaTransactionStrategy deltaTransactionStrategy = null;

	private StoreStrategy checkStrategy() {
		StoreStrategy currentStrategy = strategy;
		currentStrategy = mergeStrategies(currentStrategy, transformToImmutable, StoreStrategy.STATE);
		currentStrategy = mergeStrategies(currentStrategy, sharingStrategy, StoreStrategy.STATE);
		currentStrategy = mergeStrategies(currentStrategy, continousHashProvider, StoreStrategy.STATE);
		currentStrategy = mergeStrategies(currentStrategy, deltaTransactionStrategy, StoreStrategy.DELTA);
		return currentStrategy;
	}

	private StoreStrategy mergeStrategies(StoreStrategy old, StoreStrategy newStrategy) {
		if (old != null && newStrategy != null && old != newStrategy) {
			throw new IllegalArgumentException("Mixed strategy parametrization in VersionedMap builder!");
		}

		if (old != null) {
			return old;
		} else {
			return newStrategy;
		}
	}

	private StoreStrategy mergeStrategies(StoreStrategy old, Object parameter, StoreStrategy newStrategy) {
		if (parameter != null) {
			return mergeStrategies(old, newStrategy);
		} else {
			return old;
		}
	}

	@Override
	public VersionedMapStoreFactoryBuilder<K, V> defaultValue(V defaultValue) {
		this.defaultSet = true;
		this.defaultValue = defaultValue;
		return this;
	}

	@Override
	public VersionedMapStoreFactoryBuilder<K, V> strategy(StoreStrategy strategy) {
		this.strategy = strategy;
		checkStrategy();
		return this;
	}

	@Override
	public VersionedMapStoreFactoryBuilder<K, V> stateBasedImmutableWhenCommitting(boolean transformToImmutable) {
		this.transformToImmutable = transformToImmutable;
		checkStrategy();
		return this;
	}

	@Override
	public VersionedMapStoreFactoryBuilder<K, V> stateBasedSharingStrategy(SharingStrategy sharingStrategy) {
		this.sharingStrategy = sharingStrategy;
		checkStrategy();
		return this;
	}

	@Override
	public VersionedMapStoreFactoryBuilder<K, V> stateBasedHashProvider(ContinousHashProvider<K> hashProvider) {
		this.continousHashProvider = hashProvider;
		checkStrategy();
		return this;
	}

	@Override
	public VersionedMapStoreFactoryBuilder<K, V> deltaTransactionStrategy(DeltaTransactionStrategy deltaTransactionStrategy) {
		this.deltaTransactionStrategy = deltaTransactionStrategy;
		checkStrategy();
		return this;
	}

	private <T> T getOrDefault(T value, T defaultValue) {
		if(value != null) {
			return value;
		} else {
			return defaultValue;
		}
	}

	@Override
	public VersionedMapStoreFactory<K, V> build() {
		if (!defaultSet) {
			throw new IllegalArgumentException("Default value is missing!");
		}
		var strategyToUse = checkStrategy();
		if (strategyToUse == null) {
			return new DeltaBasedVersionedMapStoreFactory<>(defaultValue,
					getOrDefault(deltaTransactionStrategy, DeltaTransactionStrategy.LIST));
		}
		return switch (strategyToUse) {
			case STATE -> {
				if(continousHashProvider == null) {
					throw new IllegalArgumentException("Continuous hash provider is missing!");
				}
				yield new StateBasedVersionedMapStoreFactory<>(defaultValue,
						getOrDefault(transformToImmutable,true),
						getOrDefault(sharingStrategy, SharingStrategy.SHARED_NODE_CACHE_IN_GROUP),
						continousHashProvider);
			}
			case DELTA -> new DeltaBasedVersionedMapStoreFactory<>(defaultValue,
					getOrDefault(deltaTransactionStrategy, DeltaTransactionStrategy.LIST));
		};
	}

	@Override
	public String toString() {
		return "VersionedMapStoreBuilder{" +
				"defaultValue=" + defaultValue +
				", strategy=" + strategy +
				", stateBasedImmutableWhenCommitting=" + transformToImmutable +
				", stateBasedNodeSharingStrategy=" + sharingStrategy +
				", hashProvider=" + continousHashProvider +
				", deltaStorageStrategy=" + deltaTransactionStrategy +
				'}';
	}
}