package tools.refinery.store.model.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {
	@Test
	void modelConstructionTest() {
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, Boolean.class, false);

		var store = ModelStore.builder().symbols(person, friend).build();
		var symbols = store.getSymbols();

		assertTrue(symbols.contains(person));
		assertTrue(symbols.contains(friend));

		var other = new Symbol<>("other", 2, Integer.class, null);
		assertFalse(symbols.contains(other));
	}

	@Test
	void modelBuildingTest() {
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var age = new Symbol<>("age", 1, Integer.class, null);
		var friend = new Symbol<>("friend", 2, Boolean.class, false);

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
		assertNull(ageInterpretation.get( Tuple.of(2)));

		assertTrue(friendInterpretation.get(Tuple.of(0, 1)));
		assertFalse(friendInterpretation.get(Tuple.of(0, 5)));
	}

	@Test
	void modelBuildingArityFailTest() {
		var person = new Symbol<>("Person", 1, Boolean.class, false);

		var store = ModelStore.builder().symbols(person).build();
		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);

		final Tuple tuple3 = Tuple.of(1, 1, 1);
		assertThrows(IllegalArgumentException.class, () -> personInterpretation.put(tuple3, true));
		assertThrows(IllegalArgumentException.class, () -> personInterpretation.get(tuple3));
	}

	@Test
	void modelBuildingNullFailTest() {
		var age = new Symbol<>("age", 1, Integer.class, null);

		var store = ModelStore.builder().symbols(age).build();
		var model = store.createEmptyModel();
		var ageInterpretation = model.getInterpretation(age);

		ageInterpretation.put(Tuple.of(1), null); // valid
		assertThrows(IllegalArgumentException.class, () -> ageInterpretation.put(null, 1));
		assertThrows(IllegalArgumentException.class, () -> ageInterpretation.get(null));

	}

	@Test
	void modelUpdateTest() {
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var age = new Symbol<>("age", 1, Integer.class, null);
		var friend = new Symbol<>("friend", 2, Boolean.class, false);

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
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var friend = new Symbol<>("friend", 2, Boolean.class, false);

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

		long state1 = model.commit();

		assertFalse(model.hasUncommittedChanges());
		assertEquals(state1, model.getState());

		assertFalse(personInterpretation.get(Tuple.of(2)));
		assertFalse(friendInterpretation.get(Tuple.of(0, 2)));

		personInterpretation.put(Tuple.of(2), true);
		friendInterpretation.put(Tuple.of(0, 2), true);

		assertTrue(model.hasUncommittedChanges());
		assertEquals(state1, model.getState());

		long state2 = model.commit();

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
