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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static tools.refinery.store.map.tests.fuzz.utils.FuzzTestCollections.*;

class MultiThreadFuzzTest {
	public static final int noThreads = 10;

	private void runFuzzTest(String scenario, int seed, int steps, int maxKey, int maxValue, boolean nullDefault, int commitFrequency, VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		String[] values = MapTestEnvironment.prepareValues(maxValue, nullDefault);

		VersionedMapStore<Integer, String> store = builder.defaultValue(values[0]).build().createOne();

		// initialize runnables
		MultiThreadTestRunnable[] runnables = new MultiThreadTestRunnable[noThreads];
		for (int i = 0; i < noThreads; i++) {
			runnables[i] = new MultiThreadTestRunnable(scenario + "-T" + (i + 1), store, steps, maxKey, values, seed
					 , commitFrequency);
		}

		// initialize threads
		Thread[] threads = new Thread[noThreads];
		for (int i = 0; i < noThreads; i++) {
			threads[i] = new Thread(runnables[i]);
		}

		// start threads;
		for (int i = 0; i < noThreads; i++) {
			runnables[i].run();
			//threads[i].start();
		}

		// wait all the threads;
		for (int i = 0; i < noThreads; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				fail("Thread " + i + " interrupted.");
			}
		}

		// collect errors
		List<Throwable> errors = new LinkedList<>();
		for (int i = 0; i < noThreads; i++) {
			errors.addAll(runnables[i].getErrors());
		}

		assertEquals(Collections.EMPTY_LIST, errors);
	}

	static final String title = "MultiThread {index}/{0} Steps={1} Keys={2} Values={3} defaultNull={4} commit " +
			 "frequency={5} seed={6} config={7}";

	@ParameterizedTest(name = title)
	@MethodSource
	@Timeout(value = 10)
	@Tag("fuzz")
	void parametrizedFastFuzz(int ignoredTests, int steps, int noKeys, int noValues, boolean defaultNull,
							   int commitFrequency, int seed, VersionedMapStoreFactoryBuilder<Integer, String> builder) {
		runFuzzTest("MultiThreadS" + steps + "K" + noKeys + "V" + noValues + defaultNull + "CF" + commitFrequency +
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
		runFuzzTest("RestoreS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				nullDefault, commitFrequency, builder);
	}

	static Stream<Arguments> parametrizedSlowFuzz() {
		return FuzzTestUtils.changeStepCount(RestoreFuzzTest.parametrizedFastFuzz(), 1);
	}
}
