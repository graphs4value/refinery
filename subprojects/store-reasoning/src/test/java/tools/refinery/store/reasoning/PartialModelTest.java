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
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.query.view.ForbiddenView;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.Seed;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;

class PartialModelTest {
	@Test
	void partialModelTest() {
		var personStorage = Symbol.of("Person", 1, TruthValue.class, TruthValue.FALSE);
		var friendStorage = Symbol.of("friend", 2, TruthValue.class, TruthValue.UNKNOWN);

		var person = new PartialRelation("Person", 1);
		var friend = new PartialRelation("friend", 2);
		var lonely = new PartialRelation("lonely", 1);

		var store = ModelStore.builder()
				.with(ViatraModelQueryAdapter.builder())
				.with(ReasoningAdapter.builder()
						.initialNodeCount(4))
				.with(PartialRelationTranslator.of(ReasoningAdapter.EXISTS_SYMBOL)
						.symbol(Symbol.of("exists", 1, TruthValue.class, TruthValue.FALSE))
						.seed(Seed.builder(ReasoningAdapter.EXISTS_SYMBOL)
								.put(Tuple.of(0), TruthValue.TRUE)
								.put(Tuple.of(1), TruthValue.UNKNOWN)
								.put(Tuple.of(2), TruthValue.TRUE)
								.put(Tuple.of(3), TruthValue.TRUE)
								.build()))
				.with(PartialRelationTranslator.of(person)
						.symbol(personStorage)
						.seed(Seed.builder(personStorage)
								.put(Tuple.of(0), TruthValue.TRUE)
								.put(Tuple.of(1), TruthValue.TRUE)
								.put(Tuple.of(2), TruthValue.UNKNOWN)
								.build()))
				.with(PartialRelationTranslator.of(friend)
						.symbol(friendStorage)
						.may(Query.of((builder, p1, p2) -> builder.clause(
								may(person.call(p1)),
								may(person.call(p2)),
								// not(must(EQUALS_SYMBOL.call(p1, p2))),
								not(new ForbiddenView(friendStorage).call(p1, p2))
						)))
						.seed(Seed.builder(friendStorage)
								.put(Tuple.of(0, 1), TruthValue.TRUE)
								.put(Tuple.of(1, 2), TruthValue.FALSE)
								.build()))
				.with(PartialRelationTranslator.of(lonely)
						.query(Query.of((builder, p1) -> builder.clause(
								person.call(p1),
								not(friend.call(p1, Variable.of())))
						)))
				.build();

		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel();

		var queryAdapter = model.getAdapter(ModelQueryAdapter.class);
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		var friendInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, friend);
		assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.TRUE));
		assertThat(friendInterpretation.get(Tuple.of(1, 0)), is(TruthValue.UNKNOWN));
		assertThat(friendInterpretation.get(Tuple.of(3, 0)), is(TruthValue.FALSE));
		var friendRefiner = reasoningAdapter.getRefiner(friend);
		assertThat(friendRefiner.merge(Tuple.of(0, 1), TruthValue.FALSE), is(true));
		assertThat(friendRefiner.merge(Tuple.of(1, 0), TruthValue.TRUE), is(true));
		queryAdapter.flushChanges();
		assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.ERROR));
		assertThat(friendInterpretation.get(Tuple.of(1, 0)), is(TruthValue.TRUE));
	}
}
