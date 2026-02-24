/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.cardinalityinterval.CardinalityInterval;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.intinterval.IntBound;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.scope.ScopePropagator;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.attribute.AttributeInfo;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.metamodel.Metamodel;
import tools.refinery.store.reasoning.translator.metamodel.MetamodelTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.reasoning.translator.predicate.PredicateTranslator;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.statecoding.neighborhood.NeighborhoodCalculator;
import tools.refinery.store.tuple.Tuple;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.logic.term.intinterval.IntIntervalTerms.*;
import static tools.refinery.store.reasoning.literal.PartialLiterals.partialCheck;

class SmtReasoningTest {
	private static final CardinalityInterval PERSON_SCOPE = CardinalityIntervals.exactly(5);
	private static final IntInterval DEFAULT_AGE = IntInterval.of(0, IntBound.Infinite.POSITIVE_INFINITY);

	private final PartialRelation person = PartialSymbol.of("Person", 1);
	private final PartialRelation parents = PartialSymbol.of("parents", 2);
	private final PartialRelation invalidParentsMultiplicity = PartialSymbol.of("parents::invalidMultiplicity", 1);
	private final PartialFunction<IntInterval, BigInteger> age = PartialSymbol.of("age", 1,
			IntIntervalDomain.INSTANCE);
	private final PartialRelation isolated = PartialSymbol.of("isolated", 1);
	private final PartialRelation invalidParentAge = PartialSymbol.of("invalidParentAge", 2);

	@ParameterizedTest(name = "isolated = {0}")
	@MethodSource
	void modelGenerationTest(CardinalityInterval isolatedScope) {
		var store = getModelStore(isolatedScope);

		try (var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(getModelSeed())) {
			var bestFirst = new BestFirstStoreManager(store, 1);
			bestFirst.startExploration(model.commit(), 0);
			var solutions = bestFirst.getSolutionStore().getSolutions();
			assertThat(solutions, hasSize(1));
			model.restore(solutions.getFirst().version());

			var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
			var isolatedInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, isolated);
			var ageCursor = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, age).getAll();
			while (ageCursor.move()) {
				var key = ageCursor.getKey();
				var value = ageCursor.getValue();
				var isolated = isolatedInterpretation.get(key);
				if (isolated.may()) {
					// Isolated nodes should not have a concrete age.
					assertThat(value, is(DEFAULT_AGE));
				} else {
					// We must concretize each age with an `invalidParentAge` constraint.
					assertThat(value, hasProperty("concrete", is(true)));
				}
				assertThat(value, hasProperty("concrete", is(!isolated.may())));
			}
		}
	}

	public static Stream<Arguments> modelGenerationTest() {
		return Stream.of(
				Arguments.of(CardinalityIntervals.NONE),
				Arguments.of(CardinalityIntervals.ONE),
				Arguments.of(PERSON_SCOPE)
		);
	}

	private ModelStore getModelStore(CardinalityInterval isolatedScope) {
		var metamodel = Metamodel.builder()
				.type(person)
				.reference(parents, builder -> builder
						.source(person)
						.target(person)
						.multiplicity(CardinalityIntervals.atMost(2), invalidParentsMultiplicity))
				.attribute(age, new AttributeInfo(person, DEFAULT_AGE))
				.build();

		var p = Variable.of("p");
		var q = Variable.of("q");
		return ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder()
						.stateCodeCalculatorFactory(NeighborhoodCalculator.factory()))
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(new MetamodelTranslator(metamodel))
				.with(new PredicateTranslator(
						isolated,
						Query.of(isolated.name(), builder -> builder
								.parameter(p)
								.clause(
										person.call(p),
										not(parents.call(p, Variable.of())),
										not(parents.call(Variable.of(), p))
								)),
						List.of(person),
						Set.of(),
						false,
						isolatedScope.equals(CardinalityIntervals.NONE) ? TruthValue.FALSE : TruthValue.UNKNOWN
				))
				.with(new PredicateTranslator(
						invalidParentAge,
						Query.of(invalidParentAge.name(), builder -> builder
								.parameters(p, q)
								.clause(
										parents.call(p, q),
										partialCheck(less(
												age.call(q),
												add(age.call(p), constant(IntInterval.of(18)))))
								)),
						List.of(person, person),
						Set.of(),
						false,
						TruthValue.FALSE
				))
				.with(new ScopePropagator()
						.scope(person, PERSON_SCOPE)
						.scope(isolated, isolatedScope))
				.with(new SmtPropagator()
						.rule(
								Query.of("parent", builder -> builder
										.parameters(p, q)
										.clause(parents.call(p, q))),
								greaterEq(age.call(q), add(age.call(p), constant(IntInterval.of(18))))
						))
				.build();
	}

	private ModelSeed getModelSeed() {
		int newPerson = 0;
		return ModelSeed.builder(1)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(newPerson), CardinalityIntervals.SET))
				.seed(ContainmentHierarchyTranslator.CONTAINED_SYMBOL, builder -> builder
						.reducedValue(TruthValue.FALSE))
				.seed(ContainmentHierarchyTranslator.CONTAINER_SYMBOL, builder -> builder
						.reducedValue(TruthValue.FALSE))
				.seed(ContainmentHierarchyTranslator.CONTAINS_SYMBOL, builder -> builder
						.reducedValue(TruthValue.FALSE))
				.seed(person, builder -> builder
						.reducedValue(TruthValue.FALSE)
						.put(Tuple.of(newPerson), TruthValue.TRUE))
				.seed(parents, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(age, builder -> builder
						.reducedValue(DEFAULT_AGE))
				.build();
	}
}
