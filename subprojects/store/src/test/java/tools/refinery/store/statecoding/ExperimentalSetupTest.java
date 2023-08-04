/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding;

import org.junit.jupiter.api.Test;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.*;

class ExperimentalSetupTest {
	public static void generate(int size) {
		Symbol<Boolean> person = new Symbol<>("Person", 1, Boolean.class, false);
		Symbol<Boolean> friend = new Symbol<>("friend", 2, Boolean.class, false);

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(StateCoderAdapter
						.builder())
				.build();

		Set<Version> versions = new HashSet<>();
		Map<Integer, List<Version>> codes = new HashMap<>();

		var empty = store.createEmptyModel();
		var pI = empty.getInterpretation(person);

		for (int i = 0; i < size; i++) {
			pI.put(Tuple.of(i), true);
		}

		var emptyVersion = empty.commit();
		versions.add(emptyVersion);
		var emptyCode = empty.getAdapter(StateCoderAdapter.class).calculateModelCode();
		List<Version> emptyList = new ArrayList<>();
		emptyList.add(emptyVersion);
		codes.put(emptyCode, emptyList);

		var storeAdapter = store.getAdapter(StateCoderStoreAdapter.class);

		int dif = 0;
		int iso = 0;
		int unk = 0;

		//int step = 0

		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				var previousVersions = new HashSet<>(versions);
				for (var version : previousVersions) {

					var model = store.createModelForState(version);
					model.getInterpretation(friend).put(Tuple.of(i, j), true);

					Version version1 = model.commit();
					var stateCode = model.getAdapter(StateCoderAdapter.class).calculateStateCode();
					int code = stateCode.modelCode();
					//System.out.println(step+++" ->" +code);
					if (codes.containsKey(code)) {
						Version similar = codes.get(code).get(0);

						var outcome = storeAdapter.checkEquivalence(version1, similar);
						if (outcome == StateEquivalenceChecker.EquivalenceResult.DIFFERENT) {
							System.out.println();
							var c = model.getInterpretation(friend).getAll();
							while (c.move()) {
								System.out.println(c.getKey().toString());
							}
							System.out.println("vs");
							var c2 = store.createModelForState(similar).getInterpretation(friend).getAll();
							while (c2.move()) {
								System.out.println(c2.getKey().toString());
							}

							dif++;
						} else if (outcome == StateEquivalenceChecker.EquivalenceResult.UNKNOWN) {
							unk++;
						} else {
							iso++;
						}
					} else {
						versions.add(version1);

						List<Version> newList = new ArrayList<>();
						newList.add(version1);
						codes.put(code, newList);
					}
				}
			}
		}

		System.out.printf("v=%d i=%d d=%d u=%d\n", versions.size(), iso, dif, unk);
	}

	@Test
	void runTests() {
		for (int i = 0; i < 5; i++) {
			System.out.println("size = " + i);
			generate(i);
		}
	}
}
