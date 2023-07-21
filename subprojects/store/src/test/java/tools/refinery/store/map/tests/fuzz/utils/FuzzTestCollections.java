package tools.refinery.store.map.tests.fuzz.utils;

import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreFactoryBuilder;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

public final class FuzzTestCollections {
	public static final Object[] stepCounts = {FuzzTestUtils.FAST_STEP_COUNT};
	public static final Object[] keyCounts = {1 , 32, 32 * 32};
	public static final Object[] valueCounts = {2, 3};
	public static final Object[] nullDefaultOptions = {false, true};
	public static final Object[] commitFrequencyOptions = {1, 10, 100};
	public static final Object[] randomSeedOptions = {1};
	public static final Object[] storeConfigs = {
			// State based
			VersionedMapStore.<Integer,String>builder()
					.stateBasedImmutableWhenCommitting(true)
					.stateBasedHashProvider(MapTestEnvironment.prepareHashProvider(false))
					.stateBasedSharingStrategy(VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE),
			VersionedMapStore.<Integer,String>builder()
					.stateBasedImmutableWhenCommitting(true)
					.stateBasedHashProvider(MapTestEnvironment.prepareHashProvider(true))
					.stateBasedSharingStrategy(VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE),
			VersionedMapStore.<Integer,String>builder()
					.stateBasedImmutableWhenCommitting(false)
					.stateBasedHashProvider(MapTestEnvironment.prepareHashProvider(false))
					.stateBasedSharingStrategy(VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE),
			VersionedMapStore.<Integer,String>builder()
					.stateBasedImmutableWhenCommitting(false)
					.stateBasedHashProvider(MapTestEnvironment.prepareHashProvider(false))
					.stateBasedSharingStrategy(VersionedMapStoreFactoryBuilder.SharingStrategy.NO_NODE_CACHE),

			// Delta based
			VersionedMapStore.<Integer,String>builder()
					.deltaTransactionStrategy(VersionedMapStoreFactoryBuilder.DeltaTransactionStrategy.SET),
			VersionedMapStore.<Integer,String>builder()
					.deltaTransactionStrategy(VersionedMapStoreFactoryBuilder.DeltaTransactionStrategy.LIST)
	};
}
