/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import static org.junit.jupiter.api.Assertions.*;

class StateCoderBuildTest {
	Symbol<Boolean> person = new Symbol<>("Person", 1, Boolean.class, false);
	Symbol<Integer> age = new Symbol<>("age", 1, Integer.class, null);
	Symbol<Boolean> friend = new Symbol<>("friend", 2, Boolean.class, false);

	@Test
	void simpleStateCoderBuildTest() {
		var store = ModelStore.builder()
				.symbols(person, age, friend)
				.with(StateCoderAdapter.builder())
				.build();

		var model = store.createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);
		assertNotNull(stateCoder);

		var personI = model.getInterpretation(person);
		var friendI = model.getInterpretation(friend);
		var ageI = model.getInterpretation(age);

		fill(personI, friendI, ageI);

		stateCoder.calculateStateCode();
	}

	@Test
	void excludeTest() {
		var store = ModelStore.builder()
				.symbols(person, age, friend)
				.with(StateCoderAdapter.builder()
						.exclude(person)
						.exclude(age))
				.build();

		var model = store.createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);
		assertNotNull(stateCoder);

		var personI = model.getInterpretation(person);
		var friendI = model.getInterpretation(friend);
		var ageI = model.getInterpretation(age);
		fill(personI, friendI, ageI);

		int code = stateCoder.calculateStateCode().modelCode();

		ageI.put(Tuple.of(1), 3);
		assertEquals(code, stateCoder.calculateStateCode().modelCode());

		ageI.put(Tuple.of(1), null);
		assertEquals(code, stateCoder.calculateStateCode().modelCode());

		personI.put(Tuple.of(2), false);
		assertEquals(code, stateCoder.calculateStateCode().modelCode());
	}

	@Test
	void notIndividualTest() {
		var store = ModelStore.builder()
				.symbols(friend)
				.with(StateCoderAdapter.builder())
				.build();

		var model = store.createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);

		var friendI = model.getInterpretation(friend);

		friendI.put(Tuple.of(1, 2), true);
		int code1 = stateCoder.calculateModelCode();

		friendI.put(Tuple.of(1, 2), false);
		friendI.put(Tuple.of(2, 1), true);
		int code2 = stateCoder.calculateModelCode();

		assertEquals(code1, code2);
	}

	@Test
	void individualTest() {
		var store = ModelStore.builder()
				.symbols(friend)
				.with(StateCoderAdapter.builder()
						.individual(Tuple.of(1)))
				.build();

		var model = store.createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);

		var friendI = model.getInterpretation(friend);

		friendI.put(Tuple.of(1, 2), true);
		int code1 = stateCoder.calculateModelCode();

		friendI.put(Tuple.of(1, 2), false);
		friendI.put(Tuple.of(2, 1), true);
		int code2 = stateCoder.calculateModelCode();

		assertNotEquals(code1, code2);
	}

	@Test
	void customStateCoderTest() {
		final boolean[] called = new boolean[]{false};
		StateCodeCalculator mock = () -> {
			called[0] = true;
			return null;
		};

		var store = ModelStore.builder()
				.symbols(friend)
				.with(StateCoderAdapter.builder()
						.stateCodeCalculatorFactory((ignoredModel, ignoredInterpretations, ignoredIndividuals) ->
								mock))
				.build();

		var model = store.createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);

		stateCoder.calculateStateCode();

		assertTrue(called[0]);
	}

	@Test
	void customEquivalenceCheckerTest() {
		final boolean[] called = new boolean[]{false};
		StateEquivalenceChecker mock = (p1, p2, p3, p4, p5) -> {
			called[0] = true;
			return StateEquivalenceChecker.EquivalenceResult.UNKNOWN;
		};

		var store = ModelStore.builder()
				.symbols(friend)
				.with(StateCoderAdapter.builder()
						.stateEquivalenceChecker(mock))
				.build();

		var model = store.createEmptyModel();
		var v1 = model.commit();
		var v2 = model.commit();

		store.getAdapter(StateCoderStoreAdapter.class).checkEquivalence(v1, v2);

		assertTrue(called[0]);
	}


	private static void fill(Interpretation<Boolean> personI, Interpretation<Boolean> friendI, Interpretation<Integer> ageI) {
		personI.put(Tuple.of(1), true);
		personI.put(Tuple.of(2), true);

		ageI.put(Tuple.of(1), 5);
		ageI.put(Tuple.of(2), 4);

		friendI.put(Tuple.of(1, 2), true);
	}
}
