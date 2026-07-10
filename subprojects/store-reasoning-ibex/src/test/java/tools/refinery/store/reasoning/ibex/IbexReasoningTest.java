/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.ibex;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.intinterval.IntBound;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationAdapter;
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
import tools.refinery.store.tuple.Tuple;

import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static tools.refinery.logic.term.intinterval.IntIntervalTerms.*;

/**
 * Verifies that IBEX interval contraction narrows domains during the propagation stage.
 * <p>
 * The model has two concrete person nodes (0 = child, 1 = parent) connected by a
 * must-hold {@code parents} edge, with both ages initially {@code [0, 100]}.
 * <p>
 * The IBEX rule asserts {@code age(q) >= age(p) + 18} whenever {@code parents(p, q)} must-holds.
 * After initial propagation the parent's age lower bound must be raised to at least 18 and
 * the child's age upper bound must be lowered to at most 82.
 */
class IbexReasoningTest {
	private static final IntInterval DEFAULT_AGE = IntInterval.of(0, 100);

	private final PartialRelation person = PartialSymbol.of("Person", 1);
	private final PartialRelation parents = PartialSymbol.of("parents", 2);
	private final PartialRelation invalidParentsMultiplicity =
			PartialSymbol.of("parents::invalidMultiplicity", 1);
	private final PartialFunction<IntInterval, BigInteger> age =
			PartialSymbol.of("age", 1, IntIntervalDomain.INSTANCE);

	/**
	 * IBEX must narrow the age intervals during initial propagation:
	 * parent lower bound >= 18, child upper bound <= 82.
	 */
	@Test
	void parentAgeIntervalsNarrowedByIbex() {
		var store = buildStore();

		// createInitialModel seeds the model and runs propagation — no DSE needed.
		try (var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(buildSeed())) {
			var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
			var ageInterp = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, age);

			// Node 0 is the child, node 1 is the parent (see buildSeed).
			var childAge = ageInterp.get(Tuple.of(0));
			var parentAge = ageInterp.get(Tuple.of(1));

			// IBEX must have raised the parent's lower bound to at least 18.
			assertThat("parent lower-bound must be finite after IBEX propagation",
					parentAge.lowerBound(), instanceOf(IntBound.Finite.class));
			if (parentAge.lowerBound() instanceof IntBound.Finite(var lo)) {
				assertThat("parent age lower-bound must be >= 18",
						lo.compareTo(BigInteger.valueOf(18)) >= 0, is(true));
			}

			// IBEX must have lowered the child's upper bound to at most 82.
			assertThat("child upper-bound must be finite after IBEX propagation",
					childAge.upperBound(), instanceOf(IntBound.Finite.class));
			if (childAge.upperBound() instanceof IntBound.Finite(var hi)) {
				assertThat("child age upper-bound must be <= 82",
						hi.compareTo(BigInteger.valueOf(82)) <= 0, is(true));
			}
		}
	}

	// -------------------------------------------------------------------------
	// Store and seed builders
	// -------------------------------------------------------------------------

	private ModelStore buildStore() {
		var metamodel = Metamodel.builder()
				.type(person)
				.reference(parents, builder -> builder
						.source(person)
						.target(person)
						.multiplicity(CardinalityIntervals.atMost(1), invalidParentsMultiplicity))
				.attribute(age, new AttributeInfo(person, DEFAULT_AGE))
				.build();

		var p = Variable.of("p");
		var q = Variable.of("q");

		return ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(new MetamodelTranslator(metamodel))
				.with(new ScopePropagator()
						.scope(person, CardinalityIntervals.exactly(2)))
				.with(new IbexPropagator()
						.rule(
								// precondition: parents(p, q) must-hold
								Query.of("parent", builder -> builder
										.parameters(p, q)
										.clause(parents.call(p, q))),
								// assertedTerm: age(q) >= age(p) + 18
								greaterEq(age.call(q), add(age.call(p), constant(IntInterval.of(18))))
						))
				.build();
	}

	/**
	 * Two concrete person nodes (no multi-objects):
	 * <ul>
	 *   <li>node 0 — child person</li>
	 *   <li>node 1 — parent person</li>
	 * </ul>
	 * The {@code parents(0, 1)} edge is must-hold; all other parent pairs are FALSE.
	 * Both ages start as {@code [0, 100]}.
	 */
	private ModelSeed buildSeed() {
		int child = 0;
		int parent = 1;
		return ModelSeed.builder(2)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE))
				.seed(ContainmentHierarchyTranslator.CONTAINED_SYMBOL, builder -> builder
						.reducedValue(TruthValue.FALSE))
				.seed(ContainmentHierarchyTranslator.CONTAINER_SYMBOL, builder -> builder
						.reducedValue(TruthValue.FALSE))
				.seed(ContainmentHierarchyTranslator.CONTAINS_SYMBOL, builder -> builder
						.reducedValue(TruthValue.FALSE))
				.seed(person, builder -> builder
						.reducedValue(TruthValue.FALSE)
						.put(Tuple.of(child), TruthValue.TRUE)
						.put(Tuple.of(parent), TruthValue.TRUE))
				.seed(parents, builder -> builder
						.reducedValue(TruthValue.FALSE)
						.put(Tuple.of(child, parent), TruthValue.TRUE))
				.seed(age, builder -> builder
						.reducedValue(DEFAULT_AGE))
				.build();
	}
}
