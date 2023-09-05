/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.tests.fuzz;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.store.map.*;
import tools.refinery.store.map.tests.fuzz.utils.FuzzTestUtils;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;
import static tools.refinery.store.map.tests.fuzz.utils.FuzzTestCollections.*;

class DiffCursorFuzzTest {
	private void runFuzzTest(String scenario, int seed, int steps, int maxKey, int maxValue, boolean nullDefault,
							 int commitFrequency, boolean commitBeforeDiffCursor,
							 VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		String[] values = MapTestEnvironment.prepareValues(maxValue, nullDefault);

		VersionedMapStore<Integer, String> store = builder.defaultValue(values[0]).build().createOne();
		iterativeRandomPutsAndCommitsThenDiffCursor(scenario, store, steps, maxKey, values, seed, commitFrequency,
				commitBeforeDiffCursor);
	}

	private void iterativeRandomPutsAndCommitsThenDiffCursor(String scenario, VersionedMapStore<Integer, String> store,
															 int steps, int maxKey, String[] values, int seed,
															 int commitFrequency, boolean commitBeforeDiffCursor) {

		int largestCommit = -1;
		Map<Integer,Version> index2Version = new HashMap<>();

		{
			// 1. build a map with versions
			Random r = new Random(seed);
			VersionedMap<Integer, String> versioned = store.createMap();
			for (int i = 0; i < steps; i++) {
				int index = i + 1;
				int nextKey = r.nextInt(maxKey);
				String nextValue = values[r.nextInt(values.length)];
				try {
					versioned.put(nextKey, nextValue);
				} catch (Exception exception) {
					exception.printStackTrace();
					fail(scenario + ":" + index + ": exception happened: " + exception);
				}
				if (index % commitFrequency == 0) {
					Version version = versioned.commit();
					index2Version.put(index,version);
					largestCommit = index;
				}
				if (index % 10000 == 0)
					System.out.println(scenario + ":" + index + "/" + steps + " building finished");
			}
		}

		{
			// 2. create a non-versioned map,
			VersionedMap<Integer, String> moving = store.createMap();
			Random r2 = new Random(seed + 1);

			final int diffTravelFrequency = commitFrequency * 2;
			for (int i = 0; i < steps; i++) {
				int index = i + 1;
				if (index % diffTravelFrequency == 0) {
					// diff-travel
					int travelToVersion = r2.nextInt(largestCommit + 1);

					VersionedMap<Integer, String> oracle = store.createMap(index2Version.get(travelToVersion));

					if(commitBeforeDiffCursor) {
						moving.commit();
					}
					DiffCursor<Integer, String> diffCursor = moving.getDiffCursor(index2Version.get(travelToVersion));
					moving.putAll(diffCursor);
					moving.commit();

					MapTestEnvironment.compareTwoMaps(scenario + ":c" + index, oracle, moving);

					moving.restore(index2Version.get(travelToVersion));

				} else {
					// random puts
					int nextKey = r2.nextInt(maxKey);
					String nextValue = values[r2.nextInt(values.length)];
					try {
						moving.put(nextKey, nextValue);
					} catch (Exception exception) {
						exception.printStackTrace();
						fail(scenario + ":" + index + ": exception happened: " + exception);
					}
					if (index % 10000 == 0)
						System.out.println(scenario + ":" + index + "/" + steps + " building finished");
				}
			}
		}
	}

	public static final String title = "DiffCursor {index}/{0} Steps={1} Keys={2} Values={3} nullDefault={4} " +
			"commit frequency={5} seed={6} commit before diff={7} config={8}";

	@ParameterizedTest(name = title)
	@MethodSource
	@Timeout(value = 10)
	@Tag("fuzz")
	void parametrizedFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean nullDefault,
						  int commitFrequency, int seed, boolean commitBeforeDiffCursor,
						  VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		runFuzzTest("DiffCursorS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps,
				noKeys, noValues, nullDefault, commitFrequency, commitBeforeDiffCursor, builder);
	}

	static Stream<Arguments> parametrizedFuzz() {
		return FuzzTestUtils.permutationWithSize(new Object[]{100}, keyCounts, valueCounts, nullDefaultOptions,
				commitFrequencyOptions, randomSeedOptions, new Object[]{false,true}, storeConfigs);
	}

	@ParameterizedTest(name = title)
	@MethodSource
	@Tag("fuzz")
	@Tag("slow")
	void parametrizedSlowFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
			int seed, boolean commitBeforeDiffCursor, VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		runFuzzTest("DiffCursorS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				nullDefault, commitFrequency, commitBeforeDiffCursor, builder);
	}

	static Stream<Arguments> parametrizedSlowFuzz() {
		return FuzzTestUtils.changeStepCount(parametrizedFuzz(), 1);
	}
}
