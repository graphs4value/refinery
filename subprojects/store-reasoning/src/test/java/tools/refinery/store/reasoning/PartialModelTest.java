/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.query.view.ForbiddenView;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.reasoning.ReasoningAdapter.EQUALS_SYMBOL;
import static tools.refinery.store.reasoning.ReasoningAdapter.EXISTS_SYMBOL;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;

class PartialModelTest {
	@Test
	void partialModelTest() {
		var person = new PartialRelation("Person", 1);
		var friend = new PartialRelation("friend", 2);
		var lonely = new PartialRelation("lonely", 1);

		var personStorage = Symbol.of("Person", 1, TruthValue.class, TruthValue.FALSE);
		var friendStorage = Symbol.of("friend", 2, TruthValue.class, TruthValue.UNKNOWN);

		var store = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(PartialRelationTranslator.of(person)
						.symbol(personStorage))
				.with(PartialRelationTranslator.of(friend)
						.symbol(friendStorage)
						.may(Query.of("mayFriend", (builder, p1, p2) -> builder.clause(
								may(person.call(p1)),
								may(person.call(p2)),
								not(must(EQUALS_SYMBOL.call(p1, p2))),
								not(new ForbiddenView(friendStorage).call(p1, p2))
						))))
				.with(PartialRelationTranslator.of(lonely)
						.query(Query.of("lonely", (builder, p1) -> builder.clause(
								person.call(p1),
								not(friend.call(p1, Variable.of())))
						)))
				.build();

		var modelSeed = ModelSeed.builder(4)
				.seed(EXISTS_SYMBOL, builder -> builder
						.put(Tuple.of(0), TruthValue.TRUE)
						.put(Tuple.of(1), TruthValue.UNKNOWN)
						.put(Tuple.of(2), TruthValue.TRUE)
						.put(Tuple.of(3), TruthValue.TRUE))
				.seed(EQUALS_SYMBOL, builder -> builder
						.put(Tuple.of(0, 0), TruthValue.TRUE)
						.put(Tuple.of(1, 1), TruthValue.UNKNOWN)
						.put(Tuple.of(2, 2), TruthValue.UNKNOWN)
						.put(Tuple.of(3, 3), TruthValue.TRUE))
				.seed(person, builder -> builder
						.put(Tuple.of(0), TruthValue.TRUE)
						.put(Tuple.of(1), TruthValue.TRUE)
						.put(Tuple.of(2), TruthValue.UNKNOWN))
				.seed(friend, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(1, 2), TruthValue.FALSE))
				.build();
		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);

		var queryAdapter = model.getAdapter(ModelQueryAdapter.class);
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		var friendInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, friend);
		var friendRefiner = reasoningAdapter.getRefiner(friend);

		assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.TRUE));
		assertThat(friendInterpretation.get(Tuple.of(1, 0)), is(TruthValue.UNKNOWN));
		assertThat(friendInterpretation.get(Tuple.of(3, 0)), is(TruthValue.FALSE));

		assertThat(friendRefiner.merge(Tuple.of(0, 1), TruthValue.FALSE), is(true));
		assertThat(friendRefiner.merge(Tuple.of(1, 0), TruthValue.TRUE), is(true));
		var splitResult = reasoningAdapter.split(1);
		assertThat(splitResult, not(nullValue()));
		var newPerson = splitResult.get(0);
		queryAdapter.flushChanges();

		assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.ERROR));
		assertThat(friendInterpretation.get(Tuple.of(1, 0)), is(TruthValue.TRUE));
		assertThat(friendInterpretation.get(Tuple.of(0, newPerson)), is(TruthValue.ERROR));
		assertThat(friendInterpretation.get(Tuple.of(newPerson, 0)), is(TruthValue.TRUE));
	}
}
