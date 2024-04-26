/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.truthvalue.TruthValue;
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
import tools.refinery.store.tuple.Tuple;

import java.util.LinkedHashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator.CONTAINED_SYMBOL;
import static tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator.CONTAINS_SYMBOL;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.COUNT_SYMBOL;

class DefaultContainmentTest {
	private final PartialRelation c1 = new PartialRelation("C1", 1);
	private final PartialRelation c2 = new PartialRelation("C2", 1);
	private final PartialRelation c3 = new PartialRelation("C2", 1);
	private final PartialRelation r1 = new PartialRelation("r1", 2);
	private final PartialRelation r2 = new PartialRelation("r2", 2);

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void defaultContainmentTest(boolean reverse) {
		var typeHierarchy = TypeHierarchy.builder()
				.type(CONTAINED_SYMBOL, true)
				.type(c1)
				.type(c2, CONTAINED_SYMBOL)
				.type(c3, CONTAINED_SYMBOL)
				.build();

		var containmentHierarchy = getContainmentHierarchy(reverse);

		var store = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(new TypeHierarchyTranslator(typeHierarchy))
				.with(new ContainmentHierarchyTranslator(containmentHierarchy))
				.build();

		var modelSeed = ModelSeed.builder(3)
				.seed(COUNT_SYMBOL, builder -> builder.reducedValue(CardinalityIntervals.ONE))
				.seed(CONTAINED_SYMBOL, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(CONTAINS_SYMBOL, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(c1, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.TRUE))
				.seed(c2, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(1), TruthValue.TRUE))
				.seed(c3, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(2), TruthValue.TRUE))
				.seed(r1, builder -> builder
						// The {@code FALSE} value here should not affect {@code r2}.
						.reducedValue(TruthValue.FALSE)
						.put(Tuple.of(0, 1), TruthValue.TRUE))
				.seed(r2, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(1, 2), TruthValue.TRUE))
				.build();

		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		var r1Interpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, r1);
		var r2Interpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, r2);

		assertThat(r1Interpretation.get(Tuple.of(0, 1)), is(TruthValue.TRUE));
		assertThat(r1Interpretation.get(Tuple.of(1, 2)), is(TruthValue.FALSE));
		assertThat(r2Interpretation.get(Tuple.of(0, 1)), is(TruthValue.FALSE));
		assertThat(r2Interpretation.get(Tuple.of(1, 2)), is(TruthValue.TRUE));
	}

	private @NotNull LinkedHashMap<PartialRelation, ContainmentInfo> getContainmentHierarchy(boolean reverse) {
		// Make sure the order of relations is retained.
		var containmentHierarchy = new LinkedHashMap<PartialRelation, ContainmentInfo>();
		if (reverse) {
			containmentHierarchy.put(r2, new ContainmentInfo(c2, UnconstrainedMultiplicity.INSTANCE, c3));
			containmentHierarchy.put(r1, new ContainmentInfo(c1, UnconstrainedMultiplicity.INSTANCE, c2));
		} else {
			containmentHierarchy.put(r1, new ContainmentInfo(c1, UnconstrainedMultiplicity.INSTANCE, c2));
			containmentHierarchy.put(r2, new ContainmentInfo(c2, UnconstrainedMultiplicity.INSTANCE, c3));
		}
		return containmentHierarchy;
	}
}
