/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.internal.state;

import tools.refinery.store.map.ContinuousHashProvider;
import tools.refinery.store.map.VersionedMapStore;

public class VersionedMapStoreStateConfiguration {

	public VersionedMapStoreStateConfiguration() {

	}
	public VersionedMapStoreStateConfiguration(boolean immutableWhenCommitting, boolean sharedNodeCacheInStore,
											   boolean sharedNodeCacheInStoreGroups, boolean versionFreeingEnabled) {
		super();
		this.immutableWhenCommitting = immutableWhenCommitting;
		this.sharedNodeCacheInStore = sharedNodeCacheInStore;
		this.sharedNodeCacheInStoreGroups = sharedNodeCacheInStoreGroups;
		this.versionFreeingEnabled = versionFreeingEnabled;
	}

	/**
	 * If true root is replaced with immutable node when committed. Frees up memory
	 * by releasing immutable nodes, but it may decrease performance by recreating
	 * immutable nodes upon changes (some evidence).
	 */
	private boolean immutableWhenCommitting = true;
	public boolean isImmutableWhenCommitting() {
		return immutableWhenCommitting;
	}

	/**
	 * If true, all sub-nodes are cached within a {@link VersionedMapStore}. It
	 * decreases the memory requirements. It may increase performance by discovering
	 * existing immutable copy of a node (some evidence). Additional overhead may
	 * decrease performance (no example found). The option permits the efficient
	 * implementation of version deletion.
	 */
	private boolean sharedNodeCacheInStore = true;
	public boolean isSharedNodeCacheInStore() {
		return sharedNodeCacheInStore;
	}

	/**
	 * If true, all sub-nodes are cached within a group of
	 * {@link VersionedMapStoreStateImpl#createSharedVersionedMapStores(int, ContinuousHashProvider, Object, VersionedMapStoreStateConfiguration)}.
	 * If {@link VersionedMapStoreStateConfiguration#sharedNodeCacheInStore} is
	 * <code>false</code>, then it has currently no impact.
	 */
	private boolean sharedNodeCacheInStoreGroups = true;
	public boolean isSharedNodeCacheInStoreGroups() {
		return sharedNodeCacheInStoreGroups;
	}

	private boolean versionFreeingEnabled = true;
	public boolean isVersionFreeingEnabled() {
		return versionFreeingEnabled;
	}
}
