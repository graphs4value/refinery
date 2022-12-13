package tools.refinery.store.map;

public class VersionedMapStoreConfiguration {

	public VersionedMapStoreConfiguration() {

	}
	public VersionedMapStoreConfiguration(boolean immutableWhenCommitting, boolean sharedNodeCacheInStore,
			boolean sharedNodeCacheInStoreGroups) {
		super();
		this.immutableWhenCommitting = immutableWhenCommitting;
		this.sharedNodeCacheInStore = sharedNodeCacheInStore;
		this.sharedNodeCacheInStoreGroups = sharedNodeCacheInStoreGroups;
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
	 * {@link VersionedMapStoreImpl#createSharedVersionedMapStores(int, ContinousHashProvider, Object, VersionedMapStoreConfiguration)}.
	 * If {@link VersionedMapStoreConfiguration#sharedNodeCacheInStore} is
	 * <code>false</code>, then it has currently no impact.
	 */
	private boolean sharedNodeCacheInStoreGroups = true;
	public boolean isSharedNodeCacheInStoreGroups() {
		return sharedNodeCacheInStoreGroups;
	}
}
