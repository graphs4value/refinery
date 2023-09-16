/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.scope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultiObjectTest {
	private static final PartialRelation person = new PartialRelation("Person", 1);

	private ModelStore store;
	private ReasoningStoreAdapter reasoningStoreAdapter;
	private Model model;
	private Interpretation<CardinalityInterval> countStorage;

	@BeforeEach
	void beforeEach() {
		store = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(PartialRelationTranslator.of(person)
						.symbol(Symbol.of("Person", 1, TruthValue.class, TruthValue.FALSE)))
				.with(new ScopePropagator()
						.scope(person, CardinalityIntervals.between(5, 15)))
				.build();
		reasoningStoreAdapter = store.getAdapter(ReasoningStoreAdapter.class);
		model = null;
		countStorage = null;
	}

	@Test
	void oneMultiObjectSatisfiableTest() {
		createModel(ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.SET))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.build());
		assertThat(countStorage.get(Tuple.of(0)), is(CardinalityIntervals.between(2, 12)));
	}

	@Test
	void oneMultiObjectExistingBoundSatisfiableTest() {
		createModel(ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.between(5, 20)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.build());
		assertThat(countStorage.get(Tuple.of(0)), is(CardinalityIntervals.between(5, 12)));
	}

	@Test
	void oneMultiObjectUnsatisfiableUpperTest() {
		var seed = ModelSeed.builder(21)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.SET))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.build();
		assertThrows(IllegalArgumentException.class, () -> reasoningStoreAdapter.createInitialModel(seed));
	}

	@Test
	void noMultiObjectSatisfiableTest() {
		createModel(ModelSeed.builder(10)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder.reducedValue(CardinalityIntervals.ONE))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.build());
		assertThat(propagate(), is(PropagationResult.UNCHANGED));
	}

	@Test
	void noMultiObjectUnsatisfiableTest() {
		var seed = ModelSeed.builder(2)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder.reducedValue(CardinalityIntervals.ONE))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.build();
		assertThrows(IllegalArgumentException.class, () -> reasoningStoreAdapter.createInitialModel(seed));
	}

	@Test
	void oneMultiObjectExistingBoundUnsatisfiableLowerTest() {
		var seed = ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.atLeast(20)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.build();
		assertThrows(IllegalArgumentException.class, () -> reasoningStoreAdapter.createInitialModel(seed));
	}

	@Test
	void oneMultiObjectExistingBoundUnsatisfiableUpperTest() {
		var seed = ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.atMost(1)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.build();
		assertThrows(IllegalArgumentException.class, () -> reasoningStoreAdapter.createInitialModel(seed));
	}

	@Test
	void twoMultiObjectsSatisfiableTest() {
		createModel(ModelSeed.builder(5)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.SET)
						.put(Tuple.of(1), CardinalityIntervals.SET))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.build());
		assertThat(countStorage.get(Tuple.of(0)), is(CardinalityIntervals.atMost(12)));
		assertThat(countStorage.get(Tuple.of(1)), is(CardinalityIntervals.atMost(12)));
	}

	@Test
	void twoMultiObjectsExistingBoundSatisfiableTest() {
		createModel(ModelSeed.builder(5)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.between(7, 20))
						.put(Tuple.of(1), CardinalityIntervals.atMost(11)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.build());
		assertThat(countStorage.get(Tuple.of(0)), is(CardinalityIntervals.between(7, 12)));
		assertThat(countStorage.get(Tuple.of(1)), is(CardinalityIntervals.atMost(5)));
	}

	@Test
	void twoMultiObjectsExistingBoundUnsatisfiableUpperTest() {
		var seed = ModelSeed.builder(5)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.between(7, 20))
						.put(Tuple.of(1), CardinalityIntervals.exactly(11)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.build();
		assertThrows(IllegalArgumentException.class, () -> reasoningStoreAdapter.createInitialModel(seed));
	}

	@Test
	void twoMultiObjectsExistingBoundUnsatisfiableLowerTest() {
		var seed = ModelSeed.builder(3)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.LONE)
						.put(Tuple.of(1), CardinalityIntervals.atMost(2)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.build();
		assertThrows(IllegalArgumentException.class, () -> reasoningStoreAdapter.createInitialModel(seed));
	}

	@Test
	void multiToSingleTest() {
		createModel(ModelSeed.builder(5)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.LONE)
						.put(Tuple.of(1), CardinalityIntervals.SET))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.build());
		assertThat(countStorage.get(Tuple.of(0)), is(CardinalityIntervals.LONE));
		assertThat(countStorage.get(Tuple.of(1)), is(CardinalityIntervals.between(1, 12)));
		countStorage.put(Tuple.of(0), CardinalityIntervals.ONE);
		assertThat(propagate(), is(PropagationResult.PROPAGATED));
		assertThat(countStorage.get(Tuple.of(1)), is(CardinalityIntervals.between(1, 11)));
		countStorage.put(Tuple.of(1), CardinalityIntervals.ONE);
		assertThat(propagate(), is(PropagationResult.UNCHANGED));
	}

	private void createModel(ModelSeed modelSeed) {
		model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		countStorage = model.getInterpretation(MultiObjectTranslator.COUNT_STORAGE);
	}

	private PropagationResult propagate() {
		return model.getAdapter(PropagationAdapter.class).propagate();
	}
}
