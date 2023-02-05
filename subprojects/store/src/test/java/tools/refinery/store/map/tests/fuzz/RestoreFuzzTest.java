package tools.refinery.store.map.tests.fuzz;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.refinery.store.map.ContinousHashProvider;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreImpl;
import tools.refinery.store.map.internal.VersionedMapImpl;
import tools.refinery.store.map.tests.fuzz.utils.FuzzTestUtils;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

class RestoreFuzzTest {
	private void runFuzzTest(String scenario, int seed, int steps, int maxKey, int maxValue,
							 boolean nullDefault, int commitFrequency,
							 boolean evilHash) {
		String[] values = MapTestEnvironment.prepareValues(maxValue, nullDefault);
		ContinousHashProvider<Integer> chp = MapTestEnvironment.prepareHashProvider(evilHash);

		VersionedMapStore<Integer, String> store = new VersionedMapStoreImpl<>(chp, values[0]);

		iterativeRandomPutsAndCommitsThenRestore(scenario, store, steps, maxKey, values, seed, commitFrequency);
	}

	private void iterativeRandomPutsAndCommitsThenRestore(String scenario, VersionedMapStore<Integer, String> store,
														  int steps, int maxKey, String[] values, int seed, int commitFrequency) {
		// 1. build a map with versions
		Random r = new Random(seed);
		VersionedMapImpl<Integer, String> versioned = (VersionedMapImpl<Integer, String>) store.createMap();
		Map<Integer, Long> index2Version = new HashMap<>();

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
				long version = versioned.commit();
				index2Version.put(i, version);
			}
			MapTestEnvironment.printStatus(scenario, index, steps, "building");
		}
		// 2. create a non-versioned and
		VersionedMapImpl<Integer, String> reference = (VersionedMapImpl<Integer, String>) store.createMap();
		r = new Random(seed);

		for (int i = 0; i < steps; i++) {
			int index = i + 1;
			int nextKey = r.nextInt(maxKey);
			String nextValue = values[r.nextInt(values.length)];
			try {
				reference.put(nextKey, nextValue);
			} catch (Exception exception) {
				exception.printStackTrace();
				fail(scenario + ":" + index + ": exception happened: " + exception);
			}
			if (index % commitFrequency == 0) {
				versioned.restore(index2Version.get(i));
				MapTestEnvironment.compareTwoMaps(scenario + ":" + index, reference, versioned);
			}
			MapTestEnvironment.printStatus(scenario, index, steps, "comparison");
		}

	}

	@ParameterizedTest(name = "Restore {index}/{0} Steps={1} Keys={2} Values={3} nullDefault={4} commit frequency={5}" +
			" seed={6} evil-hash={7}")
	@MethodSource
	@Timeout(value = 10)
	@Tag("smoke")
	void parametrizedFastFuzz(int tests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
							  int seed, boolean evilHash) {
		runFuzzTest("RestoreS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				nullDefault, commitFrequency, evilHash);
	}

	static Stream<Arguments> parametrizedFastFuzz() {
		return FuzzTestUtils.permutationWithSize(new Object[]{FuzzTestUtils.FAST_STEP_COUNT}, new Object[]{3, 32, 32 * 32},
				new Object[]{2, 3}, new Object[]{false, true}, new Object[]{1, 10, 100}, new Object[]{1, 2, 3},
				new Object[]{false, true});
	}

	@ParameterizedTest(name = "Restore {index}/{0} Steps={1} Keys={2} Values={3} nullDefault={4} commit frequency={5}" +
			" seed={6} evil-hash={7}")
	@MethodSource
	@Tag("smoke")
	@Tag("slow")
	void parametrizedSlowFuzz(int tests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
							  int seed, boolean evilHash) {
		runFuzzTest("RestoreS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				nullDefault, commitFrequency, evilHash);
	}

	static Stream<Arguments> parametrizedSlowFuzz() {
		return FuzzTestUtils.changeStepCount(RestoreFuzzTest.parametrizedFastFuzz(), 1);
	}
}
