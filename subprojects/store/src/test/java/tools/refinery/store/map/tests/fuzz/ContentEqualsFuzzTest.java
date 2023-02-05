package tools.refinery.store.map.tests.fuzz;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.store.map.*;
import tools.refinery.store.map.internal.VersionedMapImpl;
import tools.refinery.store.map.tests.fuzz.utils.FuzzTestUtils;
import tools.refinery.store.map.tests.utils.MapTestEnvironment;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ContentEqualsFuzzTest {
	private void runFuzzTest(String scenario, int seed, int steps, int maxKey, int maxValue,
							 boolean nullDefault, int commitFrequency,
							 boolean evilHash) {
		String[] values = MapTestEnvironment.prepareValues(maxValue, nullDefault);
		ContinousHashProvider<Integer> chp = MapTestEnvironment.prepareHashProvider(evilHash);

		Random r = new Random(seed);

		iterativeRandomPutsAndCommitsThenCompare(scenario, chp, steps, maxKey, values, r, commitFrequency);
	}

	private void iterativeRandomPutsAndCommitsThenCompare(String scenario, ContinousHashProvider<Integer> chp,
														  int steps, int maxKey, String[] values, Random r,
														  int commitFrequency) {
		VersionedMapStore<Integer, String> store1 = new VersionedMapStoreImpl<Integer, String>(chp, values[0]);
		VersionedMap<Integer, String> sut1 = store1.createMap();

		// Fill one map
		for (int i = 0; i < steps; i++) {
			int index1 = i + 1;
			int nextKey = r.nextInt(maxKey);
			String nextValue = values[r.nextInt(values.length)];
			try {
				sut1.put(nextKey, nextValue);
			} catch (Exception exception) {
				exception.printStackTrace();
				fail(scenario + ":" + index1 + ": exception happened: " + exception);
			}
			MapTestEnvironment.printStatus(scenario, index1, steps, "Fill");
			if (index1 % commitFrequency == 0) {
				sut1.commit();
			}
		}

		// Get the content of the first map
		List<SimpleEntry<Integer, String>> content = new LinkedList<>();
		Cursor<Integer, String> cursor = sut1.getAll();
		while (cursor.move()) {
			content.add(new SimpleEntry<>(cursor.getKey(), cursor.getValue()));
		}

		// Randomize the order of the content
		Collections.shuffle(content, r);

		VersionedMapStore<Integer, String> store2 = new VersionedMapStoreImpl<Integer, String>(chp, values[0]);
		VersionedMap<Integer, String> sut2 = store2.createMap();
		int index2 = 1;
		for (SimpleEntry<Integer, String> entry : content) {
			sut2.put(entry.getKey(), entry.getValue());
			if (index2++ % commitFrequency == 0)
				sut2.commit();
		}

		// Check the integrity of the maps
		((VersionedMapImpl<Integer, String>) sut1).checkIntegrity();
		((VersionedMapImpl<Integer, String>) sut2).checkIntegrity();

		// Compare the two maps
		MapTestEnvironment.compareTwoMaps(scenario, sut1, sut2);
	}

	@ParameterizedTest(name = "Compare {index}/{0} Steps={1} Keys={2} Values={3} defaultNull={4} commit frequency={5}" +
			"seed={6} evil-hash={7}")
	@MethodSource
	@Timeout(value = 10)
	@Tag("fuzz")
	void parametrizedFastFuzz(int tests, int steps, int noKeys, int noValues, boolean nullDefault, int commitFrequency,
							  int seed, boolean evilHash) {
		runFuzzTest("CompareS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				nullDefault, commitFrequency, evilHash);
	}

	static Stream<Arguments> parametrizedFastFuzz() {
		return FuzzTestUtils.permutationWithSize(new Object[]{FuzzTestUtils.FAST_STEP_COUNT}, new Object[]{3, 32,
						32 * 32},
				new Object[]{2, 3}, new Object[]{false,true}, new Object[]{1, 10, 100}, new Object[]{1, 2, 3},
				new Object[]{false, true});
	}

	@ParameterizedTest(name = "Compare {index}/{0} Steps={1} Keys={2} Values={3} defaultNull={4} commit frequency={5}" +
			"seed={6} evil-hash={7}")
	@MethodSource
	@Tag("fuzz")
	@Tag("slow")
	void parametrizedSlowFuzz(int tests, int steps, int noKeys, int noValues, boolean defaultNull, int commitFrequency,
							  int seed, boolean evilHash) {
		runFuzzTest("CompareS" + steps + "K" + noKeys + "V" + noValues + "s" + seed, seed, steps, noKeys, noValues,
				defaultNull, commitFrequency, evilHash);
	}

	static Stream<Arguments> parametrizedSlowFuzz() {
		return FuzzTestUtils.changeStepCount(parametrizedFastFuzz(), 1);
	}
}
