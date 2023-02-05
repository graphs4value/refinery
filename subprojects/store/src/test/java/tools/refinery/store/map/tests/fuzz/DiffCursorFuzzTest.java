package tools.refinery.store.map.tests.fuzz;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.refinery.store.map.ContinousHashProvider;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreImpl;
import tools.refinery.store.map.internal.VersionedMapImpl;
import tools.refinery.store.map.tests.fuzz.utils.FuzzTestUtils;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

class DiffCursorFuzzTest {
	private void runFuzzTest(String scenario, int seed, int steps, int maxKey, int maxValue,
							 boolean nullDefault, int commitFrequency,
							 boolean evilHash) {
		String[] values = MapTestEnvironment.prepareValues(maxValue, nullDefault);
		ContinousHashProvider<Integer> chp = MapTestEnvironment.prepareHashProvider(evilHash);

		VersionedMapStore<Integer, String> store = new VersionedMapStoreImpl<>(chp, values[0]);
		iterativeRandomPutsAndCommitsThenDiffCursor(scenario, store, steps, maxKey, values, seed, commitFrequency);
	}

	private void iterativeRandomPutsAndCommitsThenDiffCursor(String scenario, VersionedMapStore<Integer, String> store,
															 int steps, int maxKey, String[] values, int seed, int commitFrequency) {
		// 1. build a map with versions
		Random r = new Random(seed);
		VersionedMapImpl<Integer, String> versioned = (VersionedMapImpl<Integer, String>) store.createMap();
		int largestCommit = -1;

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
				largestCommit = (int) version;
			}
			if (index % 10000 == 0)
				System.out.println(scenario + ":" + index + "/" + steps + " building finished");
		}
		// 2. create a non-versioned map,
		VersionedMapImpl<Integer, String> moving = (VersionedMapImpl<Integer, String>) store.createMap();
		Random r2 = new Random(seed + 1);

		final int diffTravelFrequency = commitFrequency * 2;
		for (int i = 0; i < steps; i++) {
			int index = i + 1;
			if (index % diffTravelFrequency == 0) {
				// diff-travel
				long travelToVersion = r2.nextInt(largestCommit + 1);
				DiffCursor<Integer, String> diffCursor = moving.getDiffCursor(travelToVersion);
				moving.putAll(diffCursor);

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
				if (index % commitFrequency == 0) {
					versioned.commit();
				}
				if (index % 10000 == 0)
					System.out.println(scenario + ":" + index + "/" + steps + " building finished");
			}
		}

	}

	@ParameterizedTest(name = "Mutable-Immutable Compare {index}/{0} Steps={1} Keys={2} Values={3} nullDefault={4} " +
			"commit frequency={5} seed={6} evil-hash={7}")
	@MethodSource
	@Timeout(value = 10)
	@Tag("fuzz")
	void parametrizedFuzz(int tests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
						  int seed,
						  boolean evilHash) {
		runFuzzTest("MutableImmutableCompareS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps,
				noKeys, noValues, nullDefault, commitFrequency, evilHash);
	}

	static Stream<Arguments> parametrizedFuzz() {
		return FuzzTestUtils.permutationWithSize(new Object[]{FuzzTestUtils.FAST_STEP_COUNT}, new Object[]{3, 32, 32 * 32},
				new Object[]{2, 3}, new Object[]{false, true}, new Object[]{1, 10, 100}, new Object[]{1, 2, 3},
				new Object[]{false, true});
	}

	@ParameterizedTest(name = "Mutable-Immutable Compare {index}/{0} Steps={1} Keys={2} Values={3} nullDefault={4} " +
			"commit frequency={5} seed={6} evil-hash={7}")
	@MethodSource
	@Tag("fuzz")
	@Tag("slow")
	void parametrizedSlowFuzz(int tests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
							  int seed,
							  boolean evilHash) {
		runFuzzTest("MutableImmutableCompareS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				nullDefault, commitFrequency, evilHash);
	}

	static Stream<Arguments> parametrizedSlowFuzz() {
		return FuzzTestUtils.changeStepCount(parametrizedFuzz(), 1);
	}
}
