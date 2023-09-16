/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.reasoning.translator.multiplicity.UnconstrainedMultiplicity;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchy;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchyTranslator;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.tuple.Tuple;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator.CONTAINED_SYMBOL;
import static tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator.CONTAINS_SYMBOL;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.COUNT_SYMBOL;

class ContainmentHierarchyTranslatorTest {
	private final PartialRelation c1 = new PartialRelation("C1", 1);
	private final PartialRelation c2 = new PartialRelation("C2", 1);
	private final PartialRelation entry = new PartialRelation("entry", 2);

	private ModelStore store;

	@BeforeEach
	void beforeEach() {

		var typeHierarchy = TypeHierarchy.builder()
				.type(CONTAINED_SYMBOL, true)
				.type(c1)
				.type(c2, c1, CONTAINED_SYMBOL)
				.build();

		var containmentHierarchy = Map.of(
				entry,
				new ContainmentInfo(c1, UnconstrainedMultiplicity.INSTANCE, c2)
		);

		store = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(new TypeHierarchyTranslator(typeHierarchy))
				.with(new ContainmentHierarchyTranslator(containmentHierarchy))
				.build();
	}

	@Test
	void treeTest() {
		var modelSeed = ModelSeed.builder(3)
				.seed(COUNT_SYMBOL, builder -> builder.reducedValue(CardinalityIntervals.ONE))
				.seed(CONTAINED_SYMBOL, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(CONTAINS_SYMBOL, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(c1, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.TRUE))
				.seed(c2, builder -> builder
						.put(Tuple.of(1), TruthValue.TRUE)
						.put(Tuple.of(2), TruthValue.TRUE))
				.seed(entry, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(0, 2), TruthValue.TRUE))
				.build();

		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		var interpretation = model.getAdapter(ReasoningAdapter.class).getPartialInterpretation(Concreteness.PARTIAL,
				entry);

		assertThat(interpretation.get(Tuple.of(0, 0)), is(TruthValue.FALSE));
		assertThat(interpretation.get(Tuple.of(0, 1)), is(TruthValue.TRUE));
		assertThat(interpretation.get(Tuple.of(0, 2)), is(TruthValue.TRUE));
		assertThat(interpretation.get(Tuple.of(1, 0)), is(TruthValue.FALSE));
		assertThat(interpretation.get(Tuple.of(1, 1)), is(TruthValue.FALSE));
		assertThat(interpretation.get(Tuple.of(1, 2)), is(TruthValue.FALSE));
		assertThat(interpretation.get(Tuple.of(2, 0)), is(TruthValue.FALSE));
		assertThat(interpretation.get(Tuple.of(2, 1)), is(TruthValue.FALSE));
		assertThat(interpretation.get(Tuple.of(2, 2)), is(TruthValue.FALSE));
	}

	@Test
	void loopTest() {
		var modelSeed = ModelSeed.builder(3)
				.seed(COUNT_SYMBOL, builder -> builder.reducedValue(CardinalityIntervals.ONE))
				.seed(CONTAINED_SYMBOL, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(CONTAINS_SYMBOL, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(c1, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(c2, builder -> builder
						.put(Tuple.of(0), TruthValue.TRUE)
						.put(Tuple.of(1), TruthValue.TRUE)
						.put(Tuple.of(2), TruthValue.TRUE))
				.seed(entry, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(1, 2), TruthValue.TRUE)
						.put(Tuple.of(2, 0), TruthValue.TRUE))
				.build();

		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		var interpretation = model.getAdapter(ReasoningAdapter.class).getPartialInterpretation(Concreteness.PARTIAL,
				entry);

		assertThat(interpretation.get(Tuple.of(0, 0)), is(TruthValue.FALSE));
		assertThat(interpretation.get(Tuple.of(0, 1)), is(TruthValue.ERROR));
		assertThat(interpretation.get(Tuple.of(0, 2)), is(TruthValue.FALSE));
		assertThat(interpretation.get(Tuple.of(1, 0)), is(TruthValue.FALSE));
		assertThat(interpretation.get(Tuple.of(1, 1)), is(TruthValue.FALSE));
		assertThat(interpretation.get(Tuple.of(1, 2)), is(TruthValue.ERROR));
		assertThat(interpretation.get(Tuple.of(2, 0)), is(TruthValue.ERROR));
		assertThat(interpretation.get(Tuple.of(2, 1)), is(TruthValue.FALSE));
		assertThat(interpretation.get(Tuple.of(2, 2)), is(TruthValue.FALSE));
	}
}
