/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.refinery.language.ProblemStandaloneSetup;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.tests.ProblemInjectorProvider;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchyTranslator;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.util.LinkedHashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
@Disabled("For debugging purposes only")
class ModelGenerationTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Inject
	private ModelInitializer modelInitializer;

	@Test
	void socialNetworkTest() {
		var parsedProblem = parseHelper.parse("""
				% Metamodel
				class Person {
				    contains Post posts opposite author
				    Person friend opposite friend
				}

				class Post {
				    container Person[0..1] author opposite posts
				    Post replyTo
				}

				% Constraints
				error replyToNotFriend(Post x, Post y) <->
				    replyTo(x, y),
				    author(x, xAuthor),
				    author(y, yAuthor),
				    xAuthor != yAuthor,
				    !friend(xAuthor, yAuthor).

				error replyToCycle(Post x) <-> replyTo+(x, x).

				% Instance model
				!friend(a, b).
				author(p1, a).
				author(p2, b).

				!author(Post::new, a).

				% Scope
				scope Post = 5, Person = 5.
				""");
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(ModelVisualizerAdapter.builder()
						.withOutputPath("test_output")
						.withFormat(FileFormat.DOT)
						.withFormat(FileFormat.SVG)
//						.saveStates()
						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);

		var initialVersion = initialModel.commit();

		var bestFirst = new BestFirstStoreManager(store, 1);
		bestFirst.startExploration(initialVersion);
		var resultStore = bestFirst.getSolutionStore();
		System.out.println("states size: " + resultStore.getSolutions().size());
//		initialModel.getAdapter(ModelVisualizerAdapter.class).visualize(bestFirst.getVisualizationStore());
	}

	@Test
	void statechartTest() {
		var parsedProblem = parseHelper.parse("""
				// Metamodel
				abstract class CompositeElement {
				    contains Region[] regions
				}

				class Region {
				    contains Vertex[] vertices opposite region
				}

				abstract class Vertex {
				    container Region[0..1] region opposite vertices
				    contains Transition[] outgoingTransition opposite source
				    Transition[] incomingTransition opposite target
				}

				class Transition {
				    container Vertex[0..1] source opposite outgoingTransition
				    Vertex target opposite incomingTransition
				}

				abstract class Pseudostate extends Vertex.

				abstract class RegularState extends Vertex.

				class Entry extends Pseudostate.

				class Exit extends Pseudostate.

				class Choice extends Pseudostate.

				class FinalState extends RegularState.

				class State extends RegularState, CompositeElement.

				class Statechart extends CompositeElement.

				// Constraints

				/////////
				// Entry
				/////////

				pred entryInRegion(Region r, Entry e) <->
					vertices(r, e).

				error noEntryInRegion(Region r) <->
				    !entryInRegion(r, _).

				error multipleEntryInRegion(Region r) <->
				    entryInRegion(r, e1),
				    entryInRegion(r, e2),
				    e1 != e2.

				error incomingToEntry(Transition t, Entry e) <->
				    target(t, e).

				error noOutgoingTransitionFromEntry(Entry e) <->
				    !source(_, e).

				error multipleTransitionFromEntry(Entry e, Transition t1, Transition t2) <->
				    outgoingTransition(e, t1),
				    outgoingTransition(e, t2),
				    t1 != t2.

				/////////
				// Exit
				/////////

				error outgoingFromExit(Transition t, Exit e) <->
				    source(t, e).

				/////////
				// Final
				/////////

				error outgoingFromFinal(Transition t, FinalState e) <->
				    source(t, e).

				/////////
				// State vs Region
				/////////

				pred stateInRegion(Region r, State s) <->
				    vertices(r, s).

				error noStateInRegion(Region r) <->
				    !stateInRegion(r, _).

				/////////
				// Choice
				/////////

				error choiceHasNoOutgoing(Choice c) <->
				    !source(_, c).

				error choiceHasNoIncoming(Choice c) <->
				    !target(_, c).

				scope node = 200..210, Region = 10..*, Choice = 1..*, Statechart = 1.
				""");
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
//				.with(ModelVisualizerAdapter.builder()
//						.withOutputPath("test_output")
//						.withFormat(FileFormat.DOT)
//						.withFormat(FileFormat.SVG)
//						.saveStates()
//						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);

		var initialVersion = initialModel.commit();

		var bestFirst = new BestFirstStoreManager(store, 1);
		bestFirst.startExploration(initialVersion);
		var resultStore = bestFirst.getSolutionStore();
		System.out.println("states size: " + resultStore.getSolutions().size());

		var model = store.createModelForState(resultStore.getSolutions().get(0).version());
		var interpretation = model.getAdapter(ReasoningAdapter.class)
				.getPartialInterpretation(Concreteness.CANDIDATE, ReasoningAdapter.EXISTS_SYMBOL);
		var cursor = interpretation.getAll();
		int max = -1;
		var types = new LinkedHashMap<PartialRelation, Integer>();
		var typeInterpretation = model.getInterpretation(TypeHierarchyTranslator.TYPE_SYMBOL);
		while (cursor.move()) {
			max = Math.max(max, cursor.getKey().get(0));
			var type = typeInterpretation.get(cursor.getKey());
			if (type != null) {
				types.compute(type.candidateType(), (ignoredKey, oldValue) -> oldValue == null ? 1 : oldValue + 1);
			}
		}
		System.out.println("Model size: " + (max + 1));
		System.out.println(types);
//		initialModel.getAdapter(ModelVisualizerAdapter.class).visualize(bestFirst.getVisualizationStore());
	}

	@Test
	void filesystemTest() {
		var parsedProblem = parseHelper.parse("""
				class Filesystem {
					contains Entry root
				}

				abstract class Entry.

				class Directory extends Entry {
					contains Entry[] entries
				}

				class File extends Entry.

				Filesystem(fs).

				scope Filesystem += 0, Entry = 100.
				""");
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
//				.with(ModelVisualizerAdapter.builder()
//						.withOutputPath("test_output")
//						.withFormat(FileFormat.DOT)
//						.withFormat(FileFormat.SVG)
//						.saveStates()
//						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);

		var initialVersion = initialModel.commit();

		var bestFirst = new BestFirstStoreManager(store, 1);
		bestFirst.startExploration(initialVersion);
		var resultStore = bestFirst.getSolutionStore();
		System.out.println("states size: " + resultStore.getSolutions().size());

		var model = store.createModelForState(resultStore.getSolutions().get(0).version());
		var interpretation = model.getAdapter(ReasoningAdapter.class)
				.getPartialInterpretation(Concreteness.CANDIDATE, ReasoningAdapter.EXISTS_SYMBOL);
		var cursor = interpretation.getAll();
		int max = -1;
		var types = new LinkedHashMap<PartialRelation, Integer>();
		var typeInterpretation = model.getInterpretation(TypeHierarchyTranslator.TYPE_SYMBOL);
		while (cursor.move()) {
			max = Math.max(max, cursor.getKey().get(0));
			var type = typeInterpretation.get(cursor.getKey());
			if (type != null) {
				types.compute(type.candidateType(), (ignoredKey, oldValue) -> oldValue == null ? 1 : oldValue + 1);
			}
		}
		System.out.println("Model size: " + (max + 1));
		System.out.println(types);
//		initialModel.getAdapter(ModelVisualizerAdapter.class).visualize(bestFirst.getVisualizationStore());
	}

	public static void main(String[] args) {
		ProblemStandaloneSetup.doSetup();
		var injector = new ProblemStandaloneSetup().createInjectorAndDoEMFRegistration();
		var test = injector.getInstance(ModelGenerationTest.class);
		try {
			test.statechartTest();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
