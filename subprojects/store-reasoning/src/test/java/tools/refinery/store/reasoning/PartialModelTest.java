/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.query.view.ForbiddenView;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.seed.Seed;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.ReasoningAdapter.EQUALS_SYMBOL;
import static tools.refinery.store.reasoning.ReasoningAdapter.EXISTS_SYMBOL;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;

class PartialModelTest {
	PartialRelation person = new PartialRelation("Person", 1);
	PartialRelation friend = new PartialRelation("friend", 2);
	PartialRelation lonely = new PartialRelation("lonely", 1);

	Symbol<TruthValue> personStorage = Symbol.of("Person", 1, TruthValue.class, TruthValue.FALSE);
	Symbol<TruthValue> friendStorage = Symbol.of("friend", 2, TruthValue.class, TruthValue.UNKNOWN);

	ModelStore store =  ModelStore.builder()
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

	ModelSeed modelSeed1 = ModelSeed.builder(4)
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

	@Test
	void partialModelTest() {
		try (var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed1)) {
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

	ModelSeed modelSeed2 = ModelSeed.builder(2)
			.seed(EXISTS_SYMBOL, builder -> builder
					.put(Tuple.of(0), TruthValue.TRUE)
					.put(Tuple.of(1), TruthValue.TRUE))
			.seed(EQUALS_SYMBOL, builder -> builder
					.put(Tuple.of(0, 0), TruthValue.TRUE)
					.put(Tuple.of(1, 1), TruthValue.TRUE))
			.seed(person, builder -> builder
					.put(Tuple.of(0), TruthValue.TRUE)
					.put(Tuple.of(1), TruthValue.TRUE))
			.seed(friend, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.build();

	@Test
	void modelResetTest1() {
		try (var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed2)) {
			var queryAdapter = model.getAdapter(ModelQueryAdapter.class);
			var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);

			var friendInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, friend);
			var friendRefiner = reasoningAdapter.getRefiner(friend);
			var lonelyInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, lonely);

			// 1. Check the original values
			assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.UNKNOWN));
			assertThat(friendInterpretation.get(Tuple.of(1, 0)), is(TruthValue.UNKNOWN));
			assertThat(lonelyInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));

			// 2. Modify the values
			assertThat(friendRefiner.merge(Tuple.of(0, 1), TruthValue.TRUE), is(true));
			assertThat(friendRefiner.merge(Tuple.of(1, 0), TruthValue.TRUE), is(true));
			queryAdapter.flushChanges();

			// 3. Check the impact of the modification
			assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.TRUE));
			assertThat(friendInterpretation.get(Tuple.of(1, 0)), is(TruthValue.TRUE));
			assertThat(lonelyInterpretation.get(Tuple.of(0)), is(TruthValue.FALSE));

			// 4. Reset the seed and observe the same values as in 1.
			reasoningAdapter.resetInitialModel(modelSeed2);
			assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.UNKNOWN));
			assertThat(friendInterpretation.get(Tuple.of(1, 0)), is(TruthValue.UNKNOWN));
			assertThat(lonelyInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));
		}
	}

	@Test
	void modelResetTest2() {
		try (var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed2)) {
			var queryAdapter = model.getAdapter(ModelQueryAdapter.class);
			var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);

			var friendInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, friend);
			var friendRefiner = reasoningAdapter.getRefiner(friend);
			var lonelyInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, lonely);

			// 1. Check the values for seed 2
			assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.UNKNOWN));
			assertThat(friendInterpretation.get(Tuple.of(1, 0)), is(TruthValue.UNKNOWN));
			assertThat(lonelyInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));
			queryAdapter.flushChanges();

			// 2. Modify the values
			assertThat(friendRefiner.merge(Tuple.of(0, 1), TruthValue.TRUE), is(true));
			assertThat(friendRefiner.merge(Tuple.of(1, 0), TruthValue.TRUE), is(true));
			queryAdapter.flushChanges();

			// 3. Reset to  seed 1
			reasoningAdapter.resetInitialModel(modelSeed1);
			queryAdapter.flushChanges();
			// 3.1 observe values on seed 1
			assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.TRUE));
			assertThat(friendInterpretation.get(Tuple.of(1, 0)), is(TruthValue.UNKNOWN));
			assertThat(friendInterpretation.get(Tuple.of(3, 0)), is(TruthValue.FALSE));
			// 3.2 add random change
			assertThat(friendRefiner.merge(Tuple.of(1, 0), TruthValue.TRUE), is(true));

			// 4. Reset to seed 2, observe the same values
			reasoningAdapter.resetInitialModel(modelSeed2);
			assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.UNKNOWN));
			assertThat(friendInterpretation.get(Tuple.of(1, 0)), is(TruthValue.UNKNOWN));
			assertThat(lonelyInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));
		}
	}

	ModelSeed getEmptySeed(ReasoningStoreAdapter storeAdapter){
		var builder = ModelSeed.builder(0);
		for(var symbol : storeAdapter.getRefinablePartialSymbols()){
			if(symbol instanceof PartialSymbol<?,?> partialSymbol) {
				builder.seed(partialSymbol, Seed.Builder::build);
			}
		}
		return builder.build();
	}

	@Test
	void restoreAndResetTest() {
		var reasoning = store.getAdapter(ReasoningStoreAdapter.class);
		try (var model = reasoning.createInitialModel(getEmptySeed(reasoning))) {
			var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
			var friendInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, friend);
			var lonelyInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, lonely);

			var empty = model.commit();
			assertThat(reasoningAdapter.getNodeCount(), is(0));
			assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.FALSE));
			assertThat(lonelyInterpretation.get(Tuple.of(0)), is(TruthValue.FALSE));

			reasoningAdapter.resetInitialModel(modelSeed2);
			assertThat(reasoningAdapter.getNodeCount(), is(2));
			assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.UNKNOWN));
			assertThat(lonelyInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));

			model.restore(empty);
			assertThat(reasoningAdapter.getNodeCount(), is(0));
			assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.FALSE));
			assertThat(lonelyInterpretation.get(Tuple.of(0)), is(TruthValue.FALSE));
		}
	}

	@Test
	void iterativeTest() {
		var reasoning = store.getAdapter(ReasoningStoreAdapter.class);
		try (var model = reasoning.createInitialModel(getEmptySeed(reasoning))) {
			var queryAdapter = model.getAdapter(ModelQueryAdapter.class);
			var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
			var friendInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, friend);
			var lonelyInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, lonely);
			var friendRefiner = reasoningAdapter.getRefiner(friend);

			var empty = model.commit();
			assertThat(reasoningAdapter.getNodeCount(), is(0));
			assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.FALSE));
			assertThat(lonelyInterpretation.get(Tuple.of(0)), is(TruthValue.FALSE));

			reasoningAdapter.resetInitialModel(modelSeed2);
			assertThat(reasoningAdapter.getNodeCount(), is(2));
			assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.UNKNOWN));
			assertThat(lonelyInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));

			assertThat(friendRefiner.merge(Tuple.of(0, 1), TruthValue.TRUE), is(true));
			assertThat(friendRefiner.merge(Tuple.of(1, 0), TruthValue.TRUE), is(true));
			queryAdapter.flushChanges();
			assertThat(lonelyInterpretation.get(Tuple.of(0)), is(TruthValue.FALSE));

			model.restore(empty);
			reasoningAdapter.resetInitialModel(modelSeed1);
			assertThat(reasoningAdapter.getNodeCount(), is(4));
			assertThat(friendInterpretation.get(Tuple.of(0, 1)), is(TruthValue.TRUE));
			assertThat(lonelyInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));
		}
	}
}
