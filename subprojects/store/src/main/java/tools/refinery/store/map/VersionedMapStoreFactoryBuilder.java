/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map;

public interface VersionedMapStoreFactoryBuilder<K,V> {
	enum StoreStrategy {
		STATE, DELTA
	}

	enum DeltaTransactionStrategy {
		LIST, SET
	}

	enum SharingStrategy {
		NO_NODE_CACHE, SHARED_NODE_CACHE, SHARED_NODE_CACHE_IN_GROUP
	}

	VersionedMapStoreFactoryBuilder<K,V> defaultValue(V defaultValue);
	VersionedMapStoreFactoryBuilder<K,V> strategy(StoreStrategy strategy);
	VersionedMapStoreFactoryBuilder<K,V> versionFreeing(boolean enabled);
	VersionedMapStoreFactoryBuilder<K,V> stateBasedImmutableWhenCommitting(boolean transformToImmutable);
	VersionedMapStoreFactoryBuilder<K,V> stateBasedSharingStrategy(SharingStrategy sharingStrategy);
	VersionedMapStoreFactoryBuilder<K,V> stateBasedHashProvider(ContinuousHashProvider<K> hashProvider);
	VersionedMapStoreFactoryBuilder<K,V> deltaTransactionStrategy(DeltaTransactionStrategy deltaStrategy);

	VersionedMapStoreFactory<K,V> build();
}
