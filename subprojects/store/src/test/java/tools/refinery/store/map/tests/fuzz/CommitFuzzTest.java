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
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreFactoryBuilder;
import tools.refinery.store.map.tests.fuzz.utils.FuzzTestUtils;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;
import static tools.refinery.store.map.tests.fuzz.utils.FuzzTestCollections.*;

class CommitFuzzTest {

	private void runFuzzTest(String scenario, int seed, int steps, int maxKey, int maxValue,
							 boolean nullDefault, int commitFrequency,
							 VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		String[] values = MapTestEnvironment.prepareValues(maxValue, nullDefault);

		VersionedMapStore<Integer, String> store = builder.defaultValue(values[0]).build().createOne();
		var sut = store.createMap();
		MapTestEnvironment<Integer, String> e = new MapTestEnvironment<>(sut);

		Random r = new Random(seed);

		iterativeRandomPutsAndCommits(scenario, steps, maxKey, values, e, r, commitFrequency);
	}

	private void iterativeRandomPutsAndCommits(String scenario, int steps, int maxKey, String[] values,
											   MapTestEnvironment<Integer, String> e, Random r, int commitFrequency) {
		for (int i = 0; i < steps; i++) {
			int index = i + 1;
			int nextKey = r.nextInt(maxKey);
			String nextValue = values[r.nextInt(values.length)];
			try {
				e.put(nextKey, nextValue);
				e.checkEquivalence(scenario + ":" + index);
			} catch (Exception exception) {
				exception.printStackTrace();
				fail(scenario + ":" + index + ": exception happened: " + exception);
			}
			MapTestEnvironment.printStatus(scenario, index, steps, null);
			if (index % commitFrequency == 0) {
				e.commit();
			}
		}
	}

	public static final String title = "Commit {index}/{0} Steps={1} Keys={2} Values={3} nullDefault={4} commit frequency={5} " +
			"seed={6} config={7}";

	@ParameterizedTest(name = title)
	@MethodSource
	@Timeout(value = 10)
	@Tag("fuzz")
	void parametrizedFastFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
							  int seed, VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		runFuzzTest("CommitS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				nullDefault, commitFrequency, builder);
	}

	static Stream<Arguments> parametrizedFastFuzz() {
		return FuzzTestUtils.permutationWithSize(stepCounts, keyCounts, valueCounts, nullDefaultOptions,
				commitFrequencyOptions, randomSeedOptions, storeConfigs);
	}

	@ParameterizedTest(name = title)
	@MethodSource
	@Tag("fuzz")
	@Tag("slow")
	void parametrizedSlowFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
							  int seed, VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		runFuzzTest("CommitS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				nullDefault, commitFrequency, builder);
	}

	static Stream<Arguments> parametrizedSlowFuzz() {
		return FuzzTestUtils.changeStepCount(parametrizedFastFuzz(), 1);
	}
}
