/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
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
			// Default
			VersionedMapStore.<Integer,String>builder()
					.stateBasedHashProvider(MapTestEnvironment.prepareHashProvider(false))
					.stateBasedSharingStrategy(VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE),
			// Evil hash code test
			VersionedMapStore.<Integer,String>builder()
					.stateBasedHashProvider(MapTestEnvironment.prepareHashProvider(true))
					.stateBasedSharingStrategy(VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE),
			// No weak hashmap test
			VersionedMapStore.<Integer,String>builder()
					.versionFreeing(false)
					.stateBasedHashProvider(MapTestEnvironment.prepareHashProvider(false))
					.stateBasedSharingStrategy(VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE),
			// Copy when committing, do not hurt the work copy, share between saves.
			VersionedMapStore.<Integer,String>builder()
					.stateBasedImmutableWhenCommitting(false)
					.stateBasedHashProvider(MapTestEnvironment.prepareHashProvider(false))
					.stateBasedSharingStrategy(VersionedMapStoreFactoryBuilder.SharingStrategy.SHARED_NODE_CACHE),
			// Copy when committing, do not hurt the work copy, do not share between states.
			VersionedMapStore.<Integer,String>builder()
					.stateBasedImmutableWhenCommitting(false)
					.stateBasedHashProvider(MapTestEnvironment.prepareHashProvider(false))
					.stateBasedSharingStrategy(VersionedMapStoreFactoryBuilder.SharingStrategy.NO_NODE_CACHE),

			// Delta based
			// Set based transactions
			VersionedMapStore.<Integer,String>builder()
					.deltaTransactionStrategy(VersionedMapStoreFactoryBuilder.DeltaTransactionStrategy.SET),
			// List based transactions
			VersionedMapStore.<Integer,String>builder()
					.deltaTransactionStrategy(VersionedMapStoreFactoryBuilder.DeltaTransactionStrategy.LIST)
	};
}
