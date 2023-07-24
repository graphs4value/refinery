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

import java.util.stream.Stream;

import static tools.refinery.store.map.tests.fuzz.utils.FuzzTestCollections.*;

class SingleThreadFuzzTest {
	private void runFuzzTest(String scenario, int seed, int steps, int maxKey, int maxValue, boolean nullDefault, int commitFrequency, VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		String[] values = MapTestEnvironment.prepareValues(maxValue, nullDefault);

		VersionedMapStore<Integer, String> store = builder.defaultValue(values[0]).build().createOne();

		// initialize runnables
		MultiThreadTestRunnable runnable = new MultiThreadTestRunnable(scenario, store, steps, maxKey, values, seed, commitFrequency);

		// start threads;
		runnable.run();
	}

	static final String title = "SingleThread {index}/{0} Steps={1} Keys={2} Values={3} defaultNull={4} commit " +
			"frequency={5} seed={6} config={7}";

	@ParameterizedTest(name = title)
	@MethodSource
	@Timeout(value = 10)
	@Tag("fuzz")
	void parametrizedFastFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean defaultNull,
							  int commitFrequency, int seed, VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		runFuzzTest("SingleThreadS" + steps + "K" + noKeys + "V" + noValues + defaultNull + "CF" + commitFrequency +
				"s" + seed, seed, steps, noKeys, noValues, defaultNull, commitFrequency, builder);
	}

	static Stream<Arguments> parametrizedFastFuzz() {
		return FuzzTestUtils.permutationWithSize(stepCounts, keyCounts, valueCounts, nullDefaultOptions,
				new Object[]{10, 100}, randomSeedOptions, storeConfigs);
	}

	@ParameterizedTest(name = title)
	@MethodSource
	@Tag("fuzz")
	@Tag("slow")
	void parametrizedSlowFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean nullDefault,
							  int commitFrequency, int seed, VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		runFuzzTest("SingleThreadS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				nullDefault, commitFrequency, builder);
	}

	static Stream<Arguments> parametrizedSlowFuzz() {
		return FuzzTestUtils.changeStepCount(RestoreFuzzTest.parametrizedFastFuzz(), 1);
	}
}
