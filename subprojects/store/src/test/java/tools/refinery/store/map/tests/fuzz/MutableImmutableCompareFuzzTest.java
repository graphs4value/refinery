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

import tools.refinery.store.map.ContinuousHashProvider;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.internal.state.VersionedMapStoreStateImpl;
import tools.refinery.store.map.internal.state.VersionedMapStateImpl;
import tools.refinery.store.map.tests.fuzz.utils.FuzzTestUtils;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

class MutableImmutableCompareFuzzTest {
	private void runFuzzTest(String scenario, int seed, int steps, int maxKey, int maxValue,
							 boolean nullDefault, int commitFrequency, boolean evilHash) {
		String[] values = MapTestEnvironment.prepareValues(maxValue, nullDefault);
		ContinuousHashProvider<Integer> chp = MapTestEnvironment.prepareHashProvider(evilHash);

		VersionedMapStore<Integer, String> store = new VersionedMapStoreStateImpl<>(chp, values[0]);
		VersionedMapStateImpl<Integer, String> immutable = (VersionedMapStateImpl<Integer, String>) store.createMap();
		VersionedMapStateImpl<Integer, String> mutable = (VersionedMapStateImpl<Integer, String>) store.createMap();

		Random r = new Random(seed);

		iterativeRandomPutsAndCommitsAndCompare(scenario, immutable, mutable, steps, maxKey, values, r,
				commitFrequency);
	}

	private void iterativeRandomPutsAndCommitsAndCompare(String scenario, VersionedMapStateImpl<Integer, String> immutable,
														 VersionedMapStateImpl<Integer, String> mutable, int steps, int maxKey, String[] values, Random r,
														 int commitFrequency) {
		for (int i = 0; i < steps; i++) {
			int index = i + 1;
			int nextKey = r.nextInt(maxKey);
			String nextValue = values[r.nextInt(values.length)];
			try {
				immutable.put(nextKey, nextValue);
				mutable.put(nextKey, nextValue);
			} catch (Exception exception) {
				exception.printStackTrace();
				fail(scenario + ":" + index + ": exception happened: " + exception);
			}
			if (index % commitFrequency == 0) {
				immutable.commit();
			}
			MapTestEnvironment.compareTwoMaps(scenario + ":" + index, immutable, mutable);

			MapTestEnvironment.printStatus(scenario, index, steps, null);
		}
	}

	@ParameterizedTest(name = "Mutable-Immutable Compare {index}/{0} Steps={1} Keys={2} Values={3} nullDefault={4} " +
			"commit frequency={5} seed={6} evil-hash={7}")
	@MethodSource
	@Timeout(value = 10)
	@Tag("fuzz")
	void parametrizedFastFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
							  int seed, boolean evilHash) {
		runFuzzTest("MutableImmutableCompareS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps,
				noKeys, noValues, nullDefault, commitFrequency, evilHash);
	}

	static Stream<Arguments> parametrizedFastFuzz() {
		return FuzzTestUtils.permutationWithSize(stepCounts, keyCounts, valueCounts, nullDefaultOptions,
				commitFrequencyOptions, randomSeedOptions, new Object[]{false, true});
	}

	@ParameterizedTest(name = "Mutable-Immutable Compare {index}/{0} Steps={1} Keys={2} Values={3} nullDefault={4} " +
			"commit frequency={5} seed={6} evil-hash={7}")
	@MethodSource
	@Tag("fuzz")
	@Tag("slow")
	void parametrizedSlowFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
							  int seed, boolean evilHash) {
		runFuzzTest("MutableImmutableCompareS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps,
				noKeys, noValues, nullDefault, commitFrequency, evilHash);
	}

	static Stream<Arguments> parametrizedSlowFuzz() {
		return FuzzTestUtils.changeStepCount(MutableImmutableCompareFuzzTest.parametrizedFastFuzz(), 1);
	}
}
