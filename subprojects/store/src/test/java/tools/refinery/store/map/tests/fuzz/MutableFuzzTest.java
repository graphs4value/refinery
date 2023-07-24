/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.map.tests.fuzz;

import static org.junit.jupiter.api.Assertions.fail;
import static tools.refinery.store.map.tests.fuzz.utils.FuzzTestCollections.*;

import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.refinery.store.map.*;
import tools.refinery.store.map.tests.fuzz.utils.FuzzTestUtils;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

class MutableFuzzTest {
	private void runFuzzTest(String scenario, int seed, int steps, int maxKey, int maxValue,
							 boolean nullDefault, VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		String[] values = MapTestEnvironment.prepareValues(maxValue, nullDefault);

		VersionedMapStore<Integer, String> store = builder.defaultValue(values[0]).build().createOne();
		VersionedMap<Integer, String> sut = store.createMap();
		MapTestEnvironment<Integer, String> e = new MapTestEnvironment<>(sut);

		Random r = new Random(seed);

		iterativeRandomPuts(scenario, steps, maxKey, values, e, r);
	}

	private void iterativeRandomPuts(String scenario, int steps, int maxKey, String[] values,
									 MapTestEnvironment<Integer, String> e, Random r) {
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
		}
	}

	final String title = "Mutable {index}/{0} Steps={1} Keys={2} Values={3} defaultNull={4} seed={5} " +
			"config={6}";

	@ParameterizedTest(name = title)
	@MethodSource
	@Timeout(value = 10)
	@Tag("fuzz")
	void parametrizedFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean defaultNull, int seed,
						  VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		runFuzzTest(
				"MutableS" + steps + "K" + noKeys + "V" + noValues + "s" + seed,
				seed, steps, noKeys, noValues, defaultNull, builder);
	}

	static Stream<Arguments> parametrizedFuzz() {
		return FuzzTestUtils.permutationWithSize(stepCounts, keyCounts, valueCounts, nullDefaultOptions,
				randomSeedOptions, storeConfigs);
	}

	@ParameterizedTest(name = title)
	@MethodSource
	@Tag("fuzz")
	@Tag("slow")
	void parametrizedSlowFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean nullDefault, int seed,
							  VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		runFuzzTest(
				"MutableS" + steps + "K" + noKeys + "V" + noValues + "s" + seed,
				seed, steps, noKeys, noValues, nullDefault, builder);
	}

	static Stream<Arguments> parametrizedSlowFuzz() {
		return FuzzTestUtils.changeStepCount(parametrizedFuzz(), 1);
	}
}
