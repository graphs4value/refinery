/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.state;

import tools.refinery.store.map.*;

import java.util.*;

public class VersionedMapStoreStateImpl<K, V> implements VersionedMapStore<K, V> {
	// Configuration
	private final boolean immutableWhenCommitting;

	// Static data
	protected final ContinuousHashProvider<K> hashProvider;
	protected final V defaultValue;

	protected final Map<Node<K, V>, ImmutableNode<K, V>> nodeCache;

	public VersionedMapStoreStateImpl(ContinuousHashProvider<K> hashProvider, V defaultValue,
									  VersionedMapStoreStateConfiguration config) {
		this.immutableWhenCommitting = config.isImmutableWhenCommitting();
		this.hashProvider = hashProvider;
		this.defaultValue = defaultValue;
		if (config.isSharedNodeCacheInStore()) {
			nodeCache = createNoteCache(config);
		} else {
			nodeCache = null;
		}
	}

	private VersionedMapStoreStateImpl(ContinuousHashProvider<K> hashProvider, V defaultValue,
									   Map<Node<K, V>, ImmutableNode<K, V>> nodeCache, VersionedMapStoreStateConfiguration config) {
		this.immutableWhenCommitting = config.isImmutableWhenCommitting();
		this.hashProvider = hashProvider;
		this.defaultValue = defaultValue;
		this.nodeCache = nodeCache;
	}

	public VersionedMapStoreStateImpl(ContinuousHashProvider<K> hashProvider, V defaultValue) {
		this(hashProvider, defaultValue, new VersionedMapStoreStateConfiguration());
	}

	public static <K, V> List<VersionedMapStore<K, V>> createSharedVersionedMapStores(int amount,
																					  ContinuousHashProvider<K> hashProvider, V defaultValue,
																					  VersionedMapStoreStateConfiguration config) {
		List<VersionedMapStore<K, V>> result = new ArrayList<>(amount);
		if (config.isSharedNodeCacheInStoreGroups()) {
			Map<Node<K, V>, ImmutableNode<K, V>> nodeCache;
			if (config.isSharedNodeCacheInStore()) {
				nodeCache = createNoteCache(config);
			} else {
				nodeCache = null;
			}
			for (int i = 0; i < amount; i++) {
				result.add(new VersionedMapStoreStateImpl<>(hashProvider, defaultValue, nodeCache, config));
			}
		} else {
			for (int i = 0; i < amount; i++) {
				result.add(new VersionedMapStoreStateImpl<>(hashProvider, defaultValue, config));
			}
		}
		return result;
	}

	private static <K,V> Map<K,V> createNoteCache(VersionedMapStoreStateConfiguration config) {
		if(config.isVersionFreeingEnabled()) {
			return new WeakHashMap<>();
		} else {
			return new HashMap<>();
		}
	}

	public static <K, V> List<VersionedMapStore<K, V>> createSharedVersionedMapStores(int amount,
																					  ContinuousHashProvider<K> hashProvider, V defaultValue) {
		return createSharedVersionedMapStores(amount, hashProvider, defaultValue, new VersionedMapStoreStateConfiguration());
	}

	@Override
	public VersionedMap<K, V> createMap() {
		return new VersionedMapStateImpl<>(this, hashProvider, defaultValue);
	}

	@Override
	public VersionedMap<K, V> createMap(Version state) {
		ImmutableNode<K, V> data = revert(state);
		return new VersionedMapStateImpl<>(this, hashProvider, defaultValue, data);
	}

	@SuppressWarnings("unchecked")
	public synchronized ImmutableNode<K, V> revert(Version state) {
		return (ImmutableNode<K, V>) state;
	}

	public synchronized Version commit(Node<K, V> data, VersionedMapStateImpl<K, V> mapToUpdateRoot) {
		ImmutableNode<K, V> immutable;
		if (data != null) {
			immutable = data.toImmutable(this.nodeCache);
		} else {
			immutable = null;
		}

		if (this.immutableWhenCommitting) {
			mapToUpdateRoot.setRoot(immutable);
		}
		return immutable;
	}

	@Override
	public DiffCursor<K, V> getDiffCursor(Version fromState, Version toState) {
		VersionedMapStateImpl<K, V> map1 = (VersionedMapStateImpl<K, V>) createMap(fromState);
		VersionedMapStateImpl<K, V> map2 = (VersionedMapStateImpl<K, V>) createMap(toState);
		InOrderMapCursor<K, V> cursor1 = new InOrderMapCursor<>(map1);
		InOrderMapCursor<K, V> cursor2 = new InOrderMapCursor<>(map2);
		return new MapDiffCursor<>(this.defaultValue, cursor1, cursor2);
	}
}
