/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ConcreteSupertypeTest {
	private final PartialRelation c1 = new PartialRelation("C1", 1);
	private final PartialRelation c2 = new PartialRelation("C2", 1);

	private ModelStore store;

	@BeforeEach
	void beforeEach() {
		var typeHierarchy = TypeHierarchy.builder()
				.type(c1)
				.type(c2, c1)
				.build();

		store = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new TypeHierarchyTranslator(typeHierarchy))
				.build();
	}

	@Test
	void inheritedTypeTrueTest() {
		var seed = ModelSeed.builder(1)
				.seed(c1, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(c2, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.TRUE))
				.build();

		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed);
		var adapter = model.getAdapter(ReasoningAdapter.class);

		assertThat(adapter.getPartialInterpretation(Concreteness.PARTIAL, c1).get(Tuple.of(0)), is(TruthValue.TRUE));
		assertThat(adapter.getPartialInterpretation(Concreteness.PARTIAL, c2).get(Tuple.of(0)), is(TruthValue.TRUE));
	}

	@Test
	void inheritedTypeFalseTest() {
		var seed = ModelSeed.builder(1)
				.seed(c1, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(c2, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.FALSE))
				.build();

		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed);
		var adapter = model.getAdapter(ReasoningAdapter.class);

		assertThat(adapter.getPartialInterpretation(Concreteness.PARTIAL, c1).get(Tuple.of(0)),
				is(TruthValue.UNKNOWN));
		assertThat(adapter.getPartialInterpretation(Concreteness.PARTIAL, c2).get(Tuple.of(0)), is(TruthValue.FALSE));
	}

	@Test
	void supertypeTrueTest() {
		var seed = ModelSeed.builder(1)
				.seed(c1, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.TRUE))
				.seed(c2, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.build();

		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed);
		var adapter = model.getAdapter(ReasoningAdapter.class);

		assertThat(adapter.getPartialInterpretation(Concreteness.PARTIAL, c1).get(Tuple.of(0)), is(TruthValue.TRUE));
		assertThat(adapter.getPartialInterpretation(Concreteness.PARTIAL, c2).get(Tuple.of(0)),
				is(TruthValue.UNKNOWN));
	}

	@Test
	void supertypeFalseTest() {
		var seed = ModelSeed.builder(1)
				.seed(c1, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.FALSE))
				.seed(c2, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.build();

		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed);
		var adapter = model.getAdapter(ReasoningAdapter.class);

		assertThat(adapter.getPartialInterpretation(Concreteness.PARTIAL, c1).get(Tuple.of(0)), is(TruthValue.FALSE));
		assertThat(adapter.getPartialInterpretation(Concreteness.PARTIAL, c2).get(Tuple.of(0)), is(TruthValue.FALSE));
	}

	@Test
	void supertypeOnlyTest() {
		var seed = ModelSeed.builder(1)
				.seed(c1, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.TRUE))
				.seed(c2, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.FALSE))
				.build();

		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed);
		var adapter = model.getAdapter(ReasoningAdapter.class);

		assertThat(adapter.getPartialInterpretation(Concreteness.PARTIAL, c1).get(Tuple.of(0)), is(TruthValue.TRUE));
		assertThat(adapter.getPartialInterpretation(Concreteness.PARTIAL, c2).get(Tuple.of(0)), is(TruthValue.FALSE));
	}


	@Test
	void inheritedTypeErrorTest() {
		var seed = ModelSeed.builder(1)
				.seed(c1, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.FALSE))
				.seed(c2, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.TRUE))
				.build();

		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed);
		var adapter = model.getAdapter(ReasoningAdapter.class);

		assertThat(adapter.getPartialInterpretation(Concreteness.PARTIAL, c1).get(Tuple.of(0)), is(TruthValue.ERROR));
		assertThat(adapter.getPartialInterpretation(Concreteness.PARTIAL, c2).get(Tuple.of(0)), is(TruthValue.ERROR));
	}
}
