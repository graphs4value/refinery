/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding;

import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperimentalSetupTest {
	static class ExperimentalSetupResult {
		int versions = 0;
		int different = 0;
		int isomorphic = 0;
		int unknown = 0;

		double failureRatio() {
			return (different + 0.0) / versions;
		}

		@Override
		public String toString() {
			return "ExperimentalSetupResult{" +
					"versions=" + versions +
					", different=" + different +
					", isomorphic=" + isomorphic +
					", unknown=" + unknown +
					", ratio= " + failureRatio() +
					'}';
		}
	}

	static int MAX = 100000;

	public static ExperimentalSetupResult generate(int size, boolean permuteTypes) {
		Symbol<Boolean> person = new Symbol<>("Person", 1, Boolean.class, false);
		Symbol<Boolean> friend = new Symbol<>("friend", 2, Boolean.class, false);

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(StateCoderAdapter
						.builder())
				.build();

		Set<Version> versions = new HashSet<>();
		MutableIntObjectMap<List<Version>> codes = IntObjectMaps.mutable.empty();

		var empty = store.createEmptyModel();
		if (!permuteTypes) {
			for (int i = 0; i < size; i++) {
				empty.getInterpretation(person).put(Tuple.of(i), true);
			}
		}

		var emptyVersion = empty.commit();
		versions.add(emptyVersion);
		var emptyCode = empty.getAdapter(StateCoderAdapter.class).calculateModelCode();
		List<Version> emptyList = new ArrayList<>();
		emptyList.add(emptyVersion);
		codes.put(emptyCode, emptyList);

		var storeAdapter = store.getAdapter(StateCoderStoreAdapter.class);
		var result = new ExperimentalSetupResult();

		int steps = 0;

		if (permuteTypes) {
			for (int i = 0; i < size; i++) {
				var previousVersions = new HashSet<>(versions);
				for (var version : previousVersions) {
					var model = store.createModelForState(version);
					model.getInterpretation(person).put(Tuple.of(i), true);

					saveAsNewVersion(versions, codes, storeAdapter, result, model);

					logProgress(steps++);
					if (steps > MAX) {
						result.versions = versions.size();
						return result;
					}
				}
			}
		}

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				var previousVersions = new HashSet<>(versions);
				for (var version : previousVersions) {

					var model = store.createModelForState(version);
					model.getInterpretation(friend).put(Tuple.of(i, j), true);

					saveAsNewVersion(versions, codes, storeAdapter, result, model);

					logProgress(steps++);
					if (steps > MAX) {
						result.versions = versions.size();
						return result;
					}
				}
			}
		}

		result.versions = versions.size();
		return result;
	}

	private static void saveAsNewVersion(Set<Version> versions, MutableIntObjectMap<List<Version>> codes,
										 StateCoderStoreAdapter storeAdapter, ExperimentalSetupResult result,
										 Model model) {
		Version version1 = model.commit();

		var stateCode = model.getAdapter(StateCoderAdapter.class).calculateStateCode();
		int code = stateCode.modelCode();
		if (codes.containsKey(code)) {
			Version similar = codes.get(code).get(0);

			var outcome = storeAdapter.checkEquivalence(version1, similar);
			if (outcome == StateEquivalenceChecker.EquivalenceResult.DIFFERENT) {
				result.different++;
			} else if (outcome == StateEquivalenceChecker.EquivalenceResult.UNKNOWN) {
				result.unknown++;
			} else {
				result.isomorphic++;
			}
		} else {
			versions.add(version1);

			List<Version> newList = new ArrayList<>();
			newList.add(version1);
			codes.put(code, newList);
		}
	}

	private static void logProgress(int steps) {
		if (steps % 10000 == 0) {
			System.out.println("Steps: " + steps + " / " + MAX);
		}
	}

	static final double limit = 0.01;

	@Test
	void test0() {
		assertEquals(1, generate(0, true).versions);
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4})
	void testForSmallUntypedModels(int size) {
		var res = generate(size, false);
		System.out.println(res);
		assertTrue(res.failureRatio() < limit);
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3})
	void testForSmallTypedModels(int size) {
		var res = generate(size, true);
		System.out.println(res);
		assertTrue(res.failureRatio() < limit);
	}

	@Test
	@Tag("fuzz")
	@Tag("slow")
	void testForLargeTypedModels() {
		var res = generate(10, true);
		System.out.println(res);
		assertTrue(res.failureRatio() < limit);
	}

	@Test
	@Tag("fuzz")
	@Tag("slow")
	void testForLargeUntypedModels() {
		var res = generate(10, false);
		System.out.println(res);
		assertTrue(res.failureRatio() < limit);
	}
}
