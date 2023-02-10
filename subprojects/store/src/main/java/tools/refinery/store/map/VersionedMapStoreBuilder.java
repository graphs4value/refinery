package tools.refinery.store.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

	protected Optional<V> defaultValue = Optional.empty();
	protected StoreStrategy strategy = StoreStrategy.DELTA;
	protected Boolean stateBasedImmutableWhenCommitting = false;
	protected StateStorageStrategy stateBasedNodeSharingStrategy = StateStorageStrategy.SHARED_NODE_CACHE_IN_GROUP;
	protected Optional<ContinousHashProvider<K>> hashProvider = Optional.empty();
	protected DeltaStorageStrategy deltaStorageStrategy = DeltaStorageStrategy.LIST;

	public void setDefaultValue(V defaultValue) {
		this.defaultValue = Optional.of(defaultValue);
	}

	public void setStrategy(StoreStrategy strategy) {
		this.strategy = strategy;
	}

	public void setHashProvider(ContinousHashProvider<K> hashProvider) {
		this.hashProvider = Optional.of(hashProvider);
	}

	public void setStateBasedImmutableWhenCommitting(boolean toImmutableWhenCommitting) {
		this.stateBasedImmutableWhenCommitting = toImmutableWhenCommitting;
	}

	public void setStateBasedNodeSharingStrategy(StateStorageStrategy strategy) {
		this.stateBasedNodeSharingStrategy = strategy;
	}

	public VersionedMapStore<K, V> buildOne() {
		return switch (strategy) {
			case DELTA -> new VersionedMapStoreDeltaImpl<>(
					this.deltaStorageStrategy == DeltaStorageStrategy.SET,
					this.defaultValue.orElseThrow());
			case STATE -> new VersionedMapStoreImpl<>(
					this.hashProvider.orElseThrow(),
					this.defaultValue.orElseThrow(),
					new VersionedMapStoreConfiguration(
							this.stateBasedImmutableWhenCommitting,
							this.stateBasedNodeSharingStrategy != StateStorageStrategy.NO_NODE_CACHE,
							this.stateBasedNodeSharingStrategy == StateStorageStrategy.SHARED_NODE_CACHE_IN_GROUP));
		};
	}

	public List<VersionedMapStore<K, V>> buildGroup(int amount) {
		if (this.strategy == StoreStrategy.STATE &&
				this.stateBasedNodeSharingStrategy == StateStorageStrategy.SHARED_NODE_CACHE_IN_GROUP) {
			return VersionedMapStoreImpl.createSharedVersionedMapStores(
					amount,
					this.hashProvider.orElseThrow(),
					this.defaultValue.orElseThrow(),
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
}
