/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class StateCoderUnitTest {
	Symbol<Boolean> person = new Symbol<>("Person", 1, Boolean.class, false);
	Symbol<Integer> age = new Symbol<>("age", 1, Integer.class, null);
	Symbol<Boolean> friend = new Symbol<>("friend", 2, Boolean.class, false);
	Symbol<Boolean> parents = new Symbol<>("parents", 3, Boolean.class, false);
	Symbol<Integer> population = new Symbol<>("population", 0, Integer.class, 0);

	private Model createEmptyModel() {
		var store = ModelStore.builder()
				.symbols(person, age, friend, parents, population)
				.with(StateCoderAdapter.builder())
				.build();

		return store.createEmptyModel();
	}

	@Test
	void emptyModelCode0() {
		Model model = createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);

		assertEquals(0, stateCoder.calculateModelCode());

		var personI = model.getInterpretation(person);
		var friendI = model.getInterpretation(friend);

		personI.put(Tuple.of(1), true);
		personI.put(Tuple.of(2), true);
		friendI.put(Tuple.of(1, 2), true);

		assertNotEquals(0, stateCoder.calculateModelCode());
	}

	@Test
	void emptyObjectCode0() {
		Model model = createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);

		var personI = model.getInterpretation(person);
		var friendI = model.getInterpretation(friend);

		assertEquals(0, stateCoder.calculateObjectCode().get(1));
		assertEquals(0, stateCoder.calculateObjectCode().get(17));

		personI.put(Tuple.of(1), true);
		personI.put(Tuple.of(2), true);
		friendI.put(Tuple.of(1, 2), true);

		assertNotEquals(0, stateCoder.calculateObjectCode().get(1));
		assertEquals(0, stateCoder.calculateObjectCode().get(17));
	}

	@Test
	void nullRelationTest() {
		Model model = createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);

		var populationI = model.getInterpretation(population);

		final int hashOf0 = Objects.hashCode(0);

		assertEquals(hashOf0, stateCoder.calculateModelCode());

		populationI.put(Tuple.of(), 1);
		int code1 = stateCoder.calculateModelCode();

		assertNotEquals(hashOf0, stateCoder.calculateModelCode());

		populationI.put(Tuple.of(), 2);
		int code2 = stateCoder.calculateModelCode();

		assertNotEquals(code1, code2);

		populationI.put(Tuple.of(), 1);
		assertEquals(code1, stateCoder.calculateModelCode());
	}

	@Test
	void unaryBooleanTest() {
		Model model = createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);

		var personI = model.getInterpretation(person);

		assertEquals(0, stateCoder.calculateModelCode());

		personI.put(Tuple.of(1), true);
		int code1 = stateCoder.calculateModelCode();

		assertNotEquals(0, stateCoder.calculateModelCode());

		personI.put(Tuple.of(2), true);
		int code2 = stateCoder.calculateModelCode();

		assertNotEquals(code1, code2);

		personI.put(Tuple.of(1), false);
		assertEquals(code1, stateCoder.calculateModelCode());
	}

	@Test
	void unaryIntTest() {
		Model model = createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);

		var ageI = model.getInterpretation(age);

		assertEquals(0, stateCoder.calculateModelCode());

		ageI.put(Tuple.of(1), 4);
		int code0 = stateCoder.calculateModelCode();

		assertNotEquals(0, code0);

		ageI.put(Tuple.of(1), 5);
		int code1 = stateCoder.calculateModelCode();

		assertNotEquals(code0, code1);

		ageI.put(Tuple.of(2), 5);
		int code2 = stateCoder.calculateModelCode();

		assertNotEquals(code1, code2);

		ageI.put(Tuple.of(1), null);
		assertEquals(code1, stateCoder.calculateModelCode());
	}

	@Test
	void binaryTest() {
		Model model = createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);

		var friendI = model.getInterpretation(friend);

		assertEquals(0, stateCoder.calculateModelCode());

		friendI.put(Tuple.of(1, 2), true);
		int code1 = stateCoder.calculateModelCode();

		assertNotEquals(0, code1);

		friendI.put(Tuple.of(2, 1), true);
		int code2 = stateCoder.calculateModelCode();

		assertNotEquals(code1, code2);

		friendI.put(Tuple.of(1, 2), false);
		int code3 = stateCoder.calculateModelCode();

		assertEquals(code1, code3);
	}

	@Test
	void NaryTest() {
		Model model = createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);

		var parentsI = model.getInterpretation(parents);

		assertEquals(0, stateCoder.calculateModelCode());

		parentsI.put(Tuple.of(3, 1, 2), true);
		int code1 = stateCoder.calculateModelCode();

		assertNotEquals(0, code1);

		parentsI.put(Tuple.of(4, 1, 2), true);
		int code2 = stateCoder.calculateModelCode();

		assertNotEquals(code1, code2);

		parentsI.put(Tuple.of(3, 1, 2), false);
		int code3 = stateCoder.calculateModelCode();

		assertEquals(code1, code3);
	}
}
