package tools.refinery.store.map;

import java.util.ArrayList;
import java.util.List;

public class VersionedMapStoreBuilder<K, V> {
	public enum StoreStrategy {
		STATE, DELTA
	}

	public enum DeltaStorageStrategy {
		LIST, SET
	}

	public enum StateStorageStrategy {
		NO_NODE_CACHE, SHARED_NODE_CACHE, SHARED_NODE_CACHE_IN_GROUP
	}

	public static <K, V> VersionedMapStoreBuilder<K, V> builder() {
		return new VersionedMapStoreBuilder<>();
	}
	protected VersionedMapStoreBuilder() {
	}
	protected VersionedMapStoreBuilder(VersionedMapStoreBuilder<K, V> other) {
		this.defaultValue = other.defaultValue;
		this.defaultSet = other.defaultSet;
		this.strategy = other.strategy;
		this.stateBasedImmutableWhenCommitting = other.stateBasedImmutableWhenCommitting;
		this.stateBasedNodeSharingStrategy = other.stateBasedNodeSharingStrategy;
		this.hashProvider = other.hashProvider;
		this.deltaStorageStrategy = other.deltaStorageStrategy;
	}
	protected boolean defaultSet = false;
	protected V defaultValue = null;
	protected StoreStrategy strategy = StoreStrategy.DELTA;
	protected Boolean stateBasedImmutableWhenCommitting = false;
	protected StateStorageStrategy stateBasedNodeSharingStrategy = StateStorageStrategy.SHARED_NODE_CACHE_IN_GROUP;
	protected ContinousHashProvider<K> hashProvider = null;
	protected DeltaStorageStrategy deltaStorageStrategy = DeltaStorageStrategy.LIST;

	public VersionedMapStoreBuilder<K, V> setDefaultValue(V defaultValue) {
		var result = new VersionedMapStoreBuilder<>(this);
		result.defaultValue = defaultValue;
		result.defaultSet = true;
		return result;
	}

	public VersionedMapStoreBuilder<K, V> setStrategy(StoreStrategy strategy) {
		var result = new VersionedMapStoreBuilder<>(this);
		result.strategy = strategy;
		return result;
	}

	public VersionedMapStoreBuilder<K, V> setHashProvider(ContinousHashProvider<K> hashProvider) {
		var result = new VersionedMapStoreBuilder<>(this);
		result.hashProvider = hashProvider;
		return result;
	}

	public VersionedMapStoreBuilder<K, V> setStateBasedImmutableWhenCommitting(boolean toImmutableWhenCommitting) {
		var result = new VersionedMapStoreBuilder<>(this);
		result.stateBasedImmutableWhenCommitting = toImmutableWhenCommitting;
		return result;
	}

	public VersionedMapStoreBuilder<K, V> setStateBasedNodeSharingStrategy(StateStorageStrategy strategy) {
		var result = new VersionedMapStoreBuilder<>(this);
		result.stateBasedNodeSharingStrategy = strategy;
		return result;
	}

	public VersionedMapStoreBuilder<K, V> setDeltaStorageStrategy(DeltaStorageStrategy deltaStorageStrategy) {
		var result = new VersionedMapStoreBuilder<>(this);
		result.deltaStorageStrategy = deltaStorageStrategy;
		return result;
	}

	public VersionedMapStore<K, V> buildOne() {
		if(!defaultSet) {
			throw new IllegalStateException("Default value is missing!");
		}
		return switch (strategy) {
			case DELTA -> new VersionedMapStoreDeltaImpl<>(
					this.deltaStorageStrategy == DeltaStorageStrategy.SET,
					this.defaultValue);
			case STATE -> new VersionedMapStoreImpl<>(
					this.hashProvider,
					this.defaultValue,
					new VersionedMapStoreConfiguration(
							this.stateBasedImmutableWhenCommitting,
							this.stateBasedNodeSharingStrategy != StateStorageStrategy.NO_NODE_CACHE,
							this.stateBasedNodeSharingStrategy == StateStorageStrategy.SHARED_NODE_CACHE_IN_GROUP));
		};
	}

	public List<VersionedMapStore<K, V>> buildGroup(int amount) {
		if(!defaultSet) {
			throw new IllegalStateException("Default value is missing!");
		}
		if (this.strategy == StoreStrategy.STATE &&
				this.stateBasedNodeSharingStrategy == StateStorageStrategy.SHARED_NODE_CACHE_IN_GROUP) {
			return VersionedMapStoreImpl.createSharedVersionedMapStores(
					amount,
					this.hashProvider,
					this.defaultValue,
					new VersionedMapStoreConfiguration(
							this.stateBasedImmutableWhenCommitting,
							true,
							true));
		} else {
			List<VersionedMapStore<K, V>> result = new ArrayList<>(amount);
			for (int i = 0; i < amount; i++) {
				result.add(buildOne());
			}
			return result;
		}
	}

	@Override
	public String toString() {
		return "VersionedMapStoreBuilder{" +
				"defaultValue=" + defaultValue +
				", strategy=" + strategy +
				", stateBasedImmutableWhenCommitting=" + stateBasedImmutableWhenCommitting +
				", stateBasedNodeSharingStrategy=" + stateBasedNodeSharingStrategy +
				", hashProvider=" + hashProvider +
				", deltaStorageStrategy=" + deltaStorageStrategy +
				'}';
	}
}
