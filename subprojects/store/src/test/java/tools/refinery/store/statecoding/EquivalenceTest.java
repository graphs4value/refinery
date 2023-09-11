/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding;

import org.junit.jupiter.api.Test;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.neighbourhood.ObjectCodeImpl;
import tools.refinery.store.tuple.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EquivalenceTest {
	Symbol<Boolean> person = new Symbol<>("Person", 1, Boolean.class, false);
	Symbol<Integer> age = new Symbol<>("age", 1, Integer.class, null);
	Symbol<Boolean> friend = new Symbol<>("friend", 2, Boolean.class, false);
	Symbol<Boolean> parents = new Symbol<>("parents", 3, Boolean.class, false);
	Symbol<Integer> population = new Symbol<>("population", 0, Integer.class, 0);

	private ModelStore createStore() {
		return ModelStore.builder()
				.symbols(person, age, friend, parents, population)
				.with(StateCoderAdapter.builder())
				.build();
	}

	@Test
	void emptyModelCode0() {
		ModelStore store = createStore();
		var stateCoder = store.getAdapter(StateCoderStoreAdapter.class);
		Model model = createStore().createEmptyModel();
		Version v1 = model.commit();
		Version v2 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.ISOMORPHIC, stateCoder.checkEquivalence(v1, v2));

		var personI = model.getInterpretation(person);
		var friendI = model.getInterpretation(friend);

		personI.put(Tuple.of(1), true);
		personI.put(Tuple.of(2), true);
		friendI.put(Tuple.of(1, 2), true);

		Version v3 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.DIFFERENT, stateCoder.checkEquivalence(v1, v3));
	}

	@Test
	void nullRelationTest() {
		ModelStore store = createStore();
		var stateCoder = store.getAdapter(StateCoderStoreAdapter.class);
		Model model = createStore().createEmptyModel();

		var populationI = model.getInterpretation(population);

		Version v1 = model.commit();

		populationI.put(Tuple.of(), 1);
		Version v2 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.DIFFERENT, stateCoder.checkEquivalence(v1, v2));

		populationI.put(Tuple.of(), 2);
		Version v3 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.DIFFERENT, stateCoder.checkEquivalence(v2, v3));
	}

	@Test
	void unaryBooleanTest() {
		ModelStore store = createStore();
		var stateCoder = store.getAdapter(StateCoderStoreAdapter.class);
		Model model = createStore().createEmptyModel();

		var personI = model.getInterpretation(person);

		Version v1 = model.commit();

		personI.put(Tuple.of(1), true);
		Version v2 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.DIFFERENT, stateCoder.checkEquivalence(v1, v2));

		personI.put(Tuple.of(2), true);
		Version v3 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.DIFFERENT, stateCoder.checkEquivalence(v2, v3));

		personI.put(Tuple.of(1), false);
		Version v4 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.ISOMORPHIC, stateCoder.checkEquivalence(v2, v4));
	}

	@Test
	void unaryIntTest() {
		ModelStore store = createStore();
		var stateCoder = store.getAdapter(StateCoderStoreAdapter.class);
		Model model = createStore().createEmptyModel();

		var ageI = model.getInterpretation(age);

		ageI.put(Tuple.of(1), 3);
		Version v1 = model.commit();

		ageI.put(Tuple.of(1), 4);
		Version v2 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.DIFFERENT, stateCoder.checkEquivalence(v1, v2));

		ageI.put(Tuple.of(2), 4);
		Version v3 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.DIFFERENT, stateCoder.checkEquivalence(v2, v3));

		ageI.put(Tuple.of(1), null);
		Version v4 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.ISOMORPHIC, stateCoder.checkEquivalence(v2, v4));
	}

	@Test
	void binaryTest() {
		ModelStore store = createStore();
		var stateCoder = store.getAdapter(StateCoderStoreAdapter.class);
		Model model = createStore().createEmptyModel();

		var friendI = model.getInterpretation(friend);

		Version v1 = model.commit();

		friendI.put(Tuple.of(1, 2), true);
		Version v2 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.DIFFERENT, stateCoder.checkEquivalence(v1, v2));

		friendI.put(Tuple.of(2, 1), true);
		Version v3 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.DIFFERENT, stateCoder.checkEquivalence(v2, v3));

		friendI.put(Tuple.of(1, 2), false);
		Version v4 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.ISOMORPHIC, stateCoder.checkEquivalence(v2, v4));
	}

	@Test
	void NaryTest() {
		ModelStore store = createStore();
		var stateCoder = store.getAdapter(StateCoderStoreAdapter.class);
		Model model = createStore().createEmptyModel();

		var parentsI = model.getInterpretation(parents);

		Version v1 = model.commit();

		parentsI.put(Tuple.of(3, 1, 2), true);
		Version v2 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.DIFFERENT, stateCoder.checkEquivalence(v1, v2));

		parentsI.put(Tuple.of(4, 1, 2), true);
		Version v3 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.DIFFERENT, stateCoder.checkEquivalence(v2, v3));

		parentsI.put(Tuple.of(3, 1, 2), false);
		Version v4 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.ISOMORPHIC, stateCoder.checkEquivalence(v2, v4));
	}

	@Test
	void largeUnknownTest() {
		final int limit = 100;

		StateCodeCalculator calculator = () -> {
			var code = new ObjectCodeImpl();
			for (int i = 0; i < limit; i++) {
				code.set(i, 1);
			}
			return new StateCoderResult(1, code);
		};

		ModelStore store = ModelStore.builder()
				.symbols(person, age, friend, parents, population)
				.with(StateCoderAdapter.builder()
						.stateCodeCalculatorFactory((ignoredModel, ignoredInterpretations, ignoredIndividuals) ->
								calculator))
				.build();

		var stateCoder = store.getAdapter(StateCoderStoreAdapter.class);
		Model model = createStore().createEmptyModel();

		var personI = model.getInterpretation(person);
		var friendI = model.getInterpretation(friend);

		for (int i = 0; i < limit; i++) {
			personI.put(Tuple.of(i), true);
		}

		friendI.put(Tuple.of(11,12),true);
		var v1 = model.commit();

		friendI.put(Tuple.of(11,12),false);
		friendI.put(Tuple.of(21,22),false);
		var v2 = model.commit();

		assertEquals(StateEquivalenceChecker.EquivalenceResult.UNKNOWN, stateCoder.checkEquivalence(v1,v2));
	}
}
