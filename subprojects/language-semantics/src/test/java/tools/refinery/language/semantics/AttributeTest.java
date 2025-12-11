/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import org.junit.jupiter.api.Test;
import tools.refinery.language.tests.InjectWithRefinery;
import tools.refinery.language.tests.utils.ProblemParseHelper;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.tuple.Tuple;

import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

@InjectWithRefinery
public class AttributeTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Inject
	private ModelInitializer modelInitializer;

	@Test
	void metamodelTest() {
		var parsedProblem = parseHelper.parse("""
				abstract class Creature {
				   int age
				  	}

				  	class Human extends Creature.
				  	class Pig extends Creature.

				  	Human(Alice).
				""");
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder());
		var modelSeed = modelInitializer.createModel(problem, storeBuilder);
		var store = storeBuilder.build();
		var trace = modelInitializer.getProblemTrace();
		@SuppressWarnings("unchecked")
		var age = (PartialFunction<IntInterval, BigInteger>) trace.getPartialFunction("Creature::age");
		var alice = trace.getNodeId("Alice");

		try (var model = store.getAdapter(ReasoningStoreAdapter.class)
				.createInitialModel(modelSeed)) {
			var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
			var ageInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, age);
			var value = ageInterpretation.get(Tuple.of(alice));
			assertThat(value, is(IntInterval.UNKNOWN));
		}
	}

	@Test
	void singleArgumentPredicateTest() {
		var parsedProblem = parseHelper.parse("""
				abstract class Creature {
				   int age
				  	}

				  	class Human extends Creature.
				  	class Pig extends Creature.

				  	Human(Alice).
				  	Pig(Piglet).

				  	pred adult(Human h) <-> age(h) >= 18.
				""");
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder());
		var modelSeed = modelInitializer.createModel(problem, storeBuilder);
		var store = storeBuilder.build();
		var trace = modelInitializer.getProblemTrace();
		var adult = trace.getPartialRelation("adult");
		var alice = trace.getNodeId("Alice");
		var piglet = trace.getNodeId("Piglet");

		try (var model = store.getAdapter(ReasoningStoreAdapter.class)
				.createInitialModel(modelSeed)) {
			var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
			var adultInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, adult);
			assertThat(adultInterpretation.get(Tuple.of(alice)), is(TruthValue.UNKNOWN));
			assertThat(adultInterpretation.get(Tuple.of(piglet)), is(TruthValue.FALSE));
		}
	}

	@Test
	void twoArgumentsPredicateTest() {
		var parsedProblem = parseHelper.parse("""
				abstract class Creature {
				   int age
				  	}

				  	class Human extends Creature.
				  	class Pig extends Creature.

				  	Human(Alice).
				  	Pig(Piglet).

				  	pred younger(Human x, Human y) <-> age(x) < age(y).
				""");
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder());
		var modelSeed = modelInitializer.createModel(problem, storeBuilder);
		var store = storeBuilder.build();
		var trace = modelInitializer.getProblemTrace();
		var younger = trace.getPartialRelation("younger");
		var alice = trace.getNodeId("Alice");
		var piglet = trace.getNodeId("Piglet");

		try (var model = store.getAdapter(ReasoningStoreAdapter.class)
				.createInitialModel(modelSeed)) {
			var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
			var youngerInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, younger);
			assertThat(youngerInterpretation.get(Tuple.of(alice, alice)), is(TruthValue.UNKNOWN));
			assertThat(youngerInterpretation.get(Tuple.of(piglet, alice)), is(TruthValue.FALSE));
		}
	}

	@Test
	void attributeRangeTest() {
		var parsedProblem = parseHelper.parse("""
				abstract class Creature {
				   int age
				  	}

				  	class Human extends Creature.
				  	class Pig extends Creature.

				  	Human(Alice).
				  	Pig(Piglet).

				  	pred child(Human h) <-> age(h) >= 0 && age(h) <= 17.
				""");
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder());
		var modelSeed = modelInitializer.createModel(problem, storeBuilder);
		var store = storeBuilder.build();
		var trace = modelInitializer.getProblemTrace();
		var alice = trace.getNodeId("Alice");
		var child = trace.getPartialRelation("child");
		var piglet = trace.getNodeId("Piglet");

		try (var model = store.getAdapter(ReasoningStoreAdapter.class)
				.createInitialModel(modelSeed)) {
			var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
			var childInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, child);

			assertThat(childInterpretation.get(Tuple.of(alice)), is(TruthValue.UNKNOWN));
			assertThat(childInterpretation.get(Tuple.of(piglet)), is(TruthValue.FALSE));
		}
	}

	@Test
	void attributeOperatorTest() {
		var parsedProblem = parseHelper.parse("""
				abstract class Creature {
				   int age
				  	}

				  	class Human extends Creature.
				  	class Pig extends Creature.

				  	Human(Alice).
				  	Pig(Piglet).

				  	pred child(Human h) <-> age(h) <= 18 - 1.
				""");
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder());
		var modelSeed = modelInitializer.createModel(problem, storeBuilder);
		var store = storeBuilder.build();
		var trace = modelInitializer.getProblemTrace();
		var alice = trace.getNodeId("Alice");
		var child = trace.getPartialRelation("child");
		var piglet = trace.getNodeId("Piglet");

		try (var model = store.getAdapter(ReasoningStoreAdapter.class)
				.createInitialModel(modelSeed)) {
			var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
			var childInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, child);

			assertThat(childInterpretation.get(Tuple.of(alice)), is(TruthValue.UNKNOWN));
			assertThat(childInterpretation.get(Tuple.of(piglet)), is(TruthValue.FALSE));
		}
	}

	@Test
	void attributeAssertionTest() {
		var parsedProblem = parseHelper.parse("""
				abstract class Creature {
				   int age
				  	}

				  	class Human extends Creature.
				  	class Pig extends Creature.

				  	Human(Alice).
				  	Pig(Piglet).

				  	age(Alice): 2 * 9.

				  	pred child(Human h) <-> age(h) >= 0 && age(h) <= 17.
				""");
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder());
		var modelSeed = modelInitializer.createModel(problem, storeBuilder);
		var store = storeBuilder.build();
		var trace = modelInitializer.getProblemTrace();
		var alice = trace.getNodeId("Alice");
		var child = trace.getPartialRelation("child");
		var piglet = trace.getNodeId("Piglet");

		try (var model = store.getAdapter(ReasoningStoreAdapter.class)
				.createInitialModel(modelSeed)) {
			var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
			var childInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, child);

			assertThat(childInterpretation.get(Tuple.of(alice)), is(TruthValue.FALSE));
			assertThat(childInterpretation.get(Tuple.of(piglet)), is(TruthValue.FALSE));
		}
	}
}
