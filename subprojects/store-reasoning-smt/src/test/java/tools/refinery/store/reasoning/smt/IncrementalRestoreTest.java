/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.PartialFunctionTranslator;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static tools.refinery.logic.term.intinterval.IntIntervalTerms.*;

class IncrementalRestoreTest {
	private final PartialRelation person = new PartialRelation("Person", 1);
	private final PartialRelation edge = new PartialRelation("edge", 2);
	private final PartialFunction<IntInterval, BigInteger> age = new PartialFunction<>("age", 1,
			IntIntervalDomain.INSTANCE);
	private final Symbol<TruthValue> personStorage = Symbol.of("Person", 1, TruthValue.class, TruthValue.FALSE);
	private final Symbol<TruthValue> edgeStorage = Symbol.of("edge", 2, TruthValue.class, TruthValue.FALSE);
	private final Symbol<IntInterval> ageStorage = Symbol.of("age", 1, IntInterval.class, IntInterval.UNKNOWN);

	@ParameterizedTest
	@EnumSource(SmtSolverStrategy.class)
	void randomRestoresAndUncommittedChangesMatchFreshSolvers(SmtSolverStrategy strategy) {
		var p = tools.refinery.logic.term.Variable.of("p");
		var q = tools.refinery.logic.term.Variable.of("q");
		var store = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(PartialRelationTranslator.of(person).symbol(personStorage))
				.with(PartialRelationTranslator.of(edge).symbol(edgeStorage))
				.with(PartialFunctionTranslator.of(age).symbol(ageStorage).domain(person))
				.with(new SmtPropagator().solverStrategy(strategy).rule(
						Query.of("edgeConstraint", builder -> builder.parameters(p, q).clause(edge.call(p, q))),
						greaterEq(age.call(q), add(age.call(p), constant(IntInterval.ONE)))))
				.build();
		var seed = ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder.reducedValue(CardinalityIntervals.ONE))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.seed(edge, builder -> builder.reducedValue(TruthValue.FALSE))
				.seed(age, builder -> builder.reducedValue(IntInterval.UNKNOWN))
				.build();
		var random = new Random(0);
		try (var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed)) {
			var versions = new ArrayList<Version>();
			versions.add(model.commit());
			for (int i = 0; i < 300; i++) {
				if (random.nextInt(3) == 0) {
					model.restore(versions.get(random.nextInt(versions.size())));
				}
				for (int j = 0, count = 1 + random.nextInt(5); j < count; j++) {
					if (random.nextBoolean()) {
						int lo = random.nextInt(21) - 10;
						var value = switch (random.nextInt(8)) {
							case 0 -> IntInterval.UNKNOWN;
							case 1 -> IntInterval.ERROR;
							case 2 -> IntInterval.of(lo);
							default -> IntInterval.of(lo, lo + random.nextInt(10));
						};
						model.getInterpretation(ageStorage).put(Tuple.of(random.nextInt(4)), value);
					} else {
						var key = Tuple.of(random.nextInt(4), random.nextInt(4));
						var value = random.nextBoolean() ? TruthValue.TRUE : TruthValue.FALSE;
						model.getInterpretation(edgeStorage).put(key, value);
					}
				}

				var actualPartial = model.getAdapter(PropagationAdapter.class).propagate().isRejected();
				var actualCandidate = model.getAdapter(PropagationAdapter.class).checkConcretization().isRejected();
				var version = model.commit();
				versions.add(version);
				try (var fresh = store.createModelForState(version)) {
					fresh.getAdapter(ModelQueryAdapter.class).flushChanges();
					assertEquals(fresh.getAdapter(PropagationAdapter.class).propagate().isRejected(), actualPartial,
							"partial at step " + i);
					assertEquals(fresh.getAdapter(PropagationAdapter.class).checkConcretization().isRejected(),
							actualCandidate, "candidate at step " + i);
				}
				if (!actualCandidate && i % 10 == 0) {
					assertFalse(model.getAdapter(PropagationAdapter.class).concretize().isRejected());
					assertFalse(model.getAdapter(PropagationAdapter.class).checkConcretization().isRejected());
					model.restore(version);
				}
			}
		}
	}
}
