/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import org.junit.jupiter.api.Test;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.scope.ScopePropagator;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchy;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchyTranslator;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class CountPropagationTest {
	@Test
	void countPropagationTest() {
		var a1 = new PartialRelation("A1", 1);
		var c1 = new PartialRelation("C1", 1);
		var c2 = new PartialRelation("C2", 1);

		var typeHierarchy = TypeHierarchy.builder()
				.type(a1, true)
				.type(c1, a1)
				.type(c2, a1)
				.build();

		var store = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(new TypeHierarchyTranslator(typeHierarchy))
				.with(new ScopePropagator()
						.scope(a1, CardinalityIntervals.between(1000, 1100))
						.scope(c1, CardinalityIntervals.between(100, 150)))
				.build();

		var modelSeed = ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.SET)
						.put(Tuple.of(1), CardinalityIntervals.SET))
				.seed(a1, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(c1, builder -> builder
						.reducedValue(TruthValue.FALSE)
						.put(Tuple.of(0), TruthValue.TRUE)
						.put(Tuple.of(2), TruthValue.TRUE))
				.seed(c2, builder -> builder
						.reducedValue(TruthValue.FALSE)
						.put(Tuple.of(1), TruthValue.TRUE)
						.put(Tuple.of(3), TruthValue.TRUE))
				.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		var initialState = initialModel.commit();

		var model = store.createModelForState(initialState);
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		var propagationAdapter = model.getAdapter(PropagationAdapter.class);
		reasoningAdapter.split(0);
		assertThat(propagationAdapter.propagate(), is(PropagationResult.UNCHANGED));
	}
}
