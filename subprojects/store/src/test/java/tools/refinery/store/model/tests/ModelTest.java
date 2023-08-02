/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {
	private static final Symbol<Boolean> person = Symbol.of("Person", 1);
	private static final Symbol<Integer> age = Symbol.of("age", 1, Integer.class);
	private static final Symbol<Boolean> friend = Symbol.of("friend", 2);

	@Test
	void modelConstructionTest() {
		var store = ModelStore.builder().symbols(person, friend).build();
		var symbols = store.getSymbols();

		assertTrue(symbols.contains(person));
		assertTrue(symbols.contains(friend));

		var other = Symbol.of("other", 2, Integer.class);
		assertFalse(symbols.contains(other));
	}

	@Test
	void modelBuildingTest() {
		var store = ModelStore.builder().symbols(person, age, friend).build();
		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var friendInterpretation = model.getInterpretation(friend);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		ageInterpretation.put(Tuple.of(0), 3);
		ageInterpretation.put(Tuple.of(1), 1);
		friendInterpretation.put(Tuple.of(0, 1), true);
		friendInterpretation.put(Tuple.of(1, 0), true);

		assertTrue(personInterpretation.get(Tuple.of(0)));
		assertTrue(personInterpretation.get(Tuple.of(1)));
		assertFalse(personInterpretation.get(Tuple.of(2)));

		assertEquals(3, ageInterpretation.get(Tuple.of(0)));
		assertEquals(1, ageInterpretation.get(Tuple.of(1)));
		assertNull(ageInterpretation.get(Tuple.of(2)));

		assertTrue(friendInterpretation.get(Tuple.of(0, 1)));
		assertFalse(friendInterpretation.get(Tuple.of(0, 5)));
	}

	@Test
	void modelBuildingArityFailTest() {
		var store = ModelStore.builder().symbols(person).build();
		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);

		final Tuple tuple3 = Tuple.of(1, 1, 1);
		assertThrows(IllegalArgumentException.class, () -> personInterpretation.put(tuple3, true));
		assertThrows(IllegalArgumentException.class, () -> personInterpretation.get(tuple3));
	}

	@Test
	void modelBuildingNullFailTest() {
		var store = ModelStore.builder().symbols(age).build();
		var model = store.createEmptyModel();
		var ageInterpretation = model.getInterpretation(age);

		ageInterpretation.put(Tuple.of(1), null); // valid
		assertThrows(IllegalArgumentException.class, () -> ageInterpretation.put(null, 1));
		assertThrows(IllegalArgumentException.class, () -> ageInterpretation.get(null));

	}

	@Test
	void modelUpdateTest() {
		var store = ModelStore.builder().symbols(person, age, friend).build();
		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var friendInterpretation = model.getInterpretation(friend);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		ageInterpretation.put(Tuple.of(0), 3);
		ageInterpretation.put(Tuple.of(1), 1);
		friendInterpretation.put(Tuple.of(0, 1), true);
		friendInterpretation.put(Tuple.of(1, 0), true);

		assertEquals(3, ageInterpretation.get(Tuple.of(0)));
		assertTrue(friendInterpretation.get(Tuple.of(0, 1)));

		ageInterpretation.put(Tuple.of(0), 4);
		friendInterpretation.put(Tuple.of(0, 1), false);

		assertEquals(4, ageInterpretation.get(Tuple.of(0)));
		assertFalse(friendInterpretation.get(Tuple.of(0, 1)));
	}

	@Test
	void restoreTest() {
		var store = ModelStore.builder().symbols(person, friend).build();
		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var friendInterpretation = model.getInterpretation(friend);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		friendInterpretation.put(Tuple.of(0, 1), true);
		friendInterpretation.put(Tuple.of(1, 0), true);

		assertTrue(model.hasUncommittedChanges());
		assertEquals(Model.NO_STATE_ID, model.getState());

		Version state1 = model.commit();

		assertFalse(model.hasUncommittedChanges());
		assertEquals(state1, model.getState());

		assertFalse(personInterpretation.get(Tuple.of(2)));
		assertFalse(friendInterpretation.get(Tuple.of(0, 2)));

		personInterpretation.put(Tuple.of(2), true);
		friendInterpretation.put(Tuple.of(0, 2), true);

		assertTrue(model.hasUncommittedChanges());
		assertEquals(state1, model.getState());

		Version state2 = model.commit();

		assertFalse(model.hasUncommittedChanges());
		assertEquals(state2, model.getState());

		assertTrue(personInterpretation.get(Tuple.of(2)));
		assertTrue(friendInterpretation.get(Tuple.of(0, 2)));

		model.restore(state1);

		assertFalse(model.hasUncommittedChanges());
		assertEquals(state1, model.getState());

		assertFalse(personInterpretation.get(Tuple.of(2)));
		assertFalse(friendInterpretation.get(Tuple.of(0, 2)));

		model.restore(state2);

		assertFalse(model.hasUncommittedChanges());
		assertEquals(state2, model.getState());

		assertTrue(personInterpretation.get(Tuple.of(2)));
		assertTrue(friendInterpretation.get(Tuple.of(0, 2)));
	}
}
