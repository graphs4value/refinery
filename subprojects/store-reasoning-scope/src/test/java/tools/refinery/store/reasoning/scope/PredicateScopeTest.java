/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.metamodel.Metamodel;
import tools.refinery.store.reasoning.translator.metamodel.MetamodelTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.store.query.literal.Literals.not;

class PredicateScopeTest {
	private static final PartialRelation index = new PartialRelation("Index", 1);
	private static final PartialRelation next = new PartialRelation("next", 2);
	private static final PartialRelation nextInvalidMultiplicity = new PartialRelation("next::invalidMultiplicity", 1);
	private static final PartialRelation prev = new PartialRelation("prev", 2);
	private static final PartialRelation prevInvalidMultiplicity = new PartialRelation("prev::invalidMultiplicity", 1);
	private static final PartialRelation loop = new PartialRelation("loop", 1);
	private static final PartialRelation first = new PartialRelation("first", 1);
	private static final PartialRelation last = new PartialRelation("last", 1);

	@Tag("slow")
	@ParameterizedTest
	@ValueSource(ints = {1, 2, 3, 4, 5})
	void generateTest(int randomSeed) {
		var store = createStore();
		var newIndex = Tuple.of(0);
		var modelSeed = ModelSeed.builder(1)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(newIndex, CardinalityIntervals.SET))
				.seed(ContainmentHierarchyTranslator.CONTAINED_SYMBOL,
						builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(index, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(newIndex, TruthValue.TRUE))
				.seed(next, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(prev, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.build();
		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		var initialVersion = model.commit();
		var bestFistSearch = new BestFirstStoreManager(store, 1);
		bestFistSearch.startExploration(initialVersion, randomSeed);
		model.restore(bestFistSearch.getSolutionStore().getSolutions().get(0).version());
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		var firstInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, first);
		assertSize(firstInterpretation, 1);
		var lastInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, last);
		assertSize(lastInterpretation, 1);
	}

	@Test
	void invalidResultTest() {
		var store = createStore();
		var modelSeed = ModelSeed.builder(8)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder.reducedValue(CardinalityIntervals.ONE))
				.seed(ContainmentHierarchyTranslator.CONTAINED_SYMBOL,
						builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(ContainmentHierarchyTranslator.CONTAINS_SYMBOL,
						builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(index, builder -> builder.reducedValue(TruthValue.TRUE))
				.seed(next, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(1, 2), TruthValue.TRUE)
						.put(Tuple.of(2, 3), TruthValue.TRUE)
						.put(Tuple.of(3, 4), TruthValue.TRUE)
						.put(Tuple.of(4, 5), TruthValue.TRUE)
						.put(Tuple.of(6, 1), TruthValue.TRUE))
				.seed(prev, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.build();
		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		var firstInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, first);
		assertSize(firstInterpretation, 3);
		var lastInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, last);
		assertSize(lastInterpretation, 3);
		var designSpaceAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		assertThat(designSpaceAdapter.checkAccept(), is(false));
	}

	private ModelStore createStore() {
		var metamodel = Metamodel.builder()
				.type(index)
				.reference(next, builder -> builder
						.source(index)
						.target(index)
						.multiplicity(CardinalityIntervals.LONE, nextInvalidMultiplicity)
						.opposite(prev))
				.reference(prev, builder -> builder
						.source(index)
						.target(index)
						.multiplicity(CardinalityIntervals.LONE, prevInvalidMultiplicity)
						.opposite(next))
				.build();
		return ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(new MetamodelTranslator(metamodel))
				.with(PartialRelationTranslator.of(loop)
						.query(Query.of("loop", (builder, p1) -> builder.clause(
								index.call(p1),
								next.callTransitive(p1, p1)
						)))
						.mayNever())
				.with(PartialRelationTranslator.of(first)
						.query(Query.of("first", (builder, p1) -> builder.clause(
								index.call(p1),
								not(prev.call(p1, Variable.of()))
						))))
				.with(PartialRelationTranslator.of(last)
						.query(Query.of("last", (builder, p1) -> builder.clause(
								index.call(p1),
								not(next.call(p1, Variable.of()))
						))))
				.with(new ScopePropagator()
						.scope(index, CardinalityIntervals.exactly(8))
						.scope(first, CardinalityIntervals.ONE)
						.scope(last, CardinalityIntervals.ONE))
				.build();
	}

	private void assertSize(PartialInterpretation<TruthValue, Boolean> partialInterpretation, int expected) {
		int size = 0;
		var cursor = partialInterpretation.getAll();
		while (cursor.move()) {
			assertThat(cursor.getValue(), is(TruthValue.TRUE));
			size++;
		}
		assertThat(size, is(expected));
	}
}
