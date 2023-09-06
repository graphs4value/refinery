/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

import org.junit.jupiter.api.Test;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.transition.objectives.QueryCriteria;
import tools.refinery.store.dse.transition.objectives.QueryObjective;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.FunctionalQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.Literals;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.term.int_.IntTerms;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransitionBuildTests {
	Symbol<Boolean> person = new Symbol<>("Person", 1, Boolean.class, false);
	Symbol<Boolean> friend = new Symbol<>("friend", 2, Boolean.class, false);
	AnySymbolView personView = new KeyOnlyView<>(person);
	AnySymbolView friendView = new KeyOnlyView<>(friend);

	/*RelationalQuery areNotFriends = Query.of("areNotFriends",
			(builder, p1, p2) -> builder.clause(
					personView.call(p1),
					personView.call(p2),
					not(friendView.call(p1, p2))))*/

	RelationalQuery moreThan3Friends = Query.of("moreThan3Friends",
			(builder, tooMuchFriends) -> builder.clause(Integer.class, (numberOfFriends) -> List.of(
					numberOfFriends.assign(friendView.count(tooMuchFriends, Variable.of())),
					Literals.assume(IntTerms.less(IntTerms.constant(3), numberOfFriends)),
					personView.call(tooMuchFriends)
			)));

	RelationalQuery somebodyHasNoFriend = Query.of("somebodyHasNoFriend",
			(builder, lonely) -> builder.clause(
					personView.call(lonely),
					friendView.call(CallPolarity.NEGATIVE, lonely, Variable.of())
			));
	FunctionalQuery<Integer> numberOfFriends = FunctionalQuery.of(Integer.class,
			(builder, output) -> builder.clause(output.assign(friendView.count(Variable.of(), Variable.of()))));

	@Test
	void acceptTest() {
		Model model = getModel();

		var dse = model.getAdapter(DesignSpaceExplorationAdapter.class);
		var query = model.getAdapter(ModelQueryAdapter.class);
		var personI = model.getInterpretation(person);
		var friendI = model.getInterpretation(friend);

		assertTrue(dse.checkAccept());
		personI.put(Tuple.of(1), true);
		personI.put(Tuple.of(2), true);

		query.flushChanges();

		assertFalse(dse.checkAccept());
		friendI.put(Tuple.of(1, 2), true);
		friendI.put(Tuple.of(2, 1), true);

		query.flushChanges();

		assertTrue(dse.checkAccept());
	}

	@Test
	void includeTest() {
		Model model = getModel();

		var dse = model.getAdapter(DesignSpaceExplorationAdapter.class);
		var query = model.getAdapter(ModelQueryAdapter.class);
		var personI = model.getInterpretation(person);
		var friendI = model.getInterpretation(friend);

		assertFalse(dse.checkExclude());

		personI.put(Tuple.of(1), true);
		friendI.put(Tuple.of(1, 2), true);
		friendI.put(Tuple.of(1, 3), true);
		friendI.put(Tuple.of(1, 4), true);

		query.flushChanges();
		assertFalse(dse.checkExclude());

		personI.put(Tuple.of(5), true);
		friendI.put(Tuple.of(1, 5), true);

		query.flushChanges();
		assertTrue(dse.checkExclude());

		friendI.put(Tuple.of(1, 2), false);

		query.flushChanges();
		assertFalse(dse.checkExclude());
	}

	@Test
	void objectiveTest() {
		Model model = getModel();

		var dse = model.getAdapter(DesignSpaceExplorationAdapter.class);
		var query = model.getAdapter(ModelQueryAdapter.class);
		var friendI = model.getInterpretation(friend);

		assertEquals(0.0, dse.getObjectiveValue().get(0), 0.01);

		friendI.put(Tuple.of(1, 2), true);

		query.flushChanges();
		assertEquals(1.0, dse.getObjectiveValue().get(0), 0.01);

		friendI.put(Tuple.of(1, 3), true);
		friendI.put(Tuple.of(1, 4), true);

		query.flushChanges();
		assertEquals(3.0, dse.getObjectiveValue().get(0), 0.01);
	}

	private Model getModel() {
		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(ViatraModelQueryAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(ModificationAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder()
						.objective(new QueryObjective(numberOfFriends))
						.exclude(new QueryCriteria(moreThan3Friends, true))
						.accept(new QueryCriteria(somebodyHasNoFriend, false)))
				.build();

		return store.createEmptyModel();
	}
}
