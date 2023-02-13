package tools.refinery.store.map.tests.fuzz.utils;

import tools.refinery.store.map.VersionedMapStoreBuilder;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

public final class FuzzTestCollections {
	public static final Object[] stepCounts = {FuzzTestUtils.FAST_STEP_COUNT};
	public static final Object[] keyCounts = {1, 32, 32 * 32};
	public static final Object[] valueCounts = {2, 3};
	public static final Object[] nullDefaultOptions = {false, true};
	public static final Object[] commitFrequencyOptions = {10, 10, 100};
	public static final Object[] randomSeedOptions = {1/*, 2, 3*/};
	public static final Object[] storeConfigs = {
			// State based
			VersionedMapStoreBuilder.<Integer,String>builder()
					.setStrategy(VersionedMapStoreBuilder.StoreStrategy.STATE)
					.setStateBasedImmutableWhenCommitting(true)
					.setHashProvider(MapTestEnvironment.prepareHashProvider(false))
					.setStateBasedNodeSharingStrategy(VersionedMapStoreBuilder.StateStorageStrategy.SHARED_NODE_CACHE),
			VersionedMapStoreBuilder.<Integer,String>builder()
					.setStrategy(VersionedMapStoreBuilder.StoreStrategy.STATE)
					.setStateBasedImmutableWhenCommitting(true)
					.setHashProvider(MapTestEnvironment.prepareHashProvider(true))
					.setStateBasedNodeSharingStrategy(VersionedMapStoreBuilder.StateStorageStrategy.SHARED_NODE_CACHE),
			VersionedMapStoreBuilder.<Integer,String>builder()
					.setStrategy(VersionedMapStoreBuilder.StoreStrategy.STATE)
					.setStateBasedImmutableWhenCommitting(false)
					.setHashProvider(MapTestEnvironment.prepareHashProvider(false))
					.setStateBasedNodeSharingStrategy(VersionedMapStoreBuilder.StateStorageStrategy.SHARED_NODE_CACHE),
			VersionedMapStoreBuilder.<Integer,String>builder()
					.setStrategy(VersionedMapStoreBuilder.StoreStrategy.STATE)
					.setStateBasedImmutableWhenCommitting(false)
					.setHashProvider(MapTestEnvironment.prepareHashProvider(false))
					.setStateBasedNodeSharingStrategy(VersionedMapStoreBuilder.StateStorageStrategy.NO_NODE_CACHE),
			// Delta based
			VersionedMapStoreBuilder.<Integer,String>builder()
					.setStrategy(VersionedMapStoreBuilder.StoreStrategy.DELTA)
					.setDeltaStorageStrategy(VersionedMapStoreBuilder.DeltaStorageStrategy.SET),
			VersionedMapStoreBuilder.<Integer,String>builder()
					.setStrategy(VersionedMapStoreBuilder.StoreStrategy.DELTA)
					.setDeltaStorageStrategy(VersionedMapStoreBuilder.DeltaStorageStrategy.LIST)
	};
}
