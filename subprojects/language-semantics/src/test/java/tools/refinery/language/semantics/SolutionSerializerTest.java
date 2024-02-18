/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import tools.refinery.store.statecoding.StateCoderAdapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class SolutionSerializerTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Inject
	private Provider<ModelInitializer> initializerProvider;

	@Inject
	private Provider<SolutionSerializer> serializerProvider;

	@ParameterizedTest
	@MethodSource
	void solutionSerializerTest(String prefix, String input, String expectedOutput) throws IOException {
		var problem = parseHelper.parse(prefix + "\n" + input).problem();
		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(Set.of(Concreteness.CANDIDATE)));
		var initializer = initializerProvider.get();
		var modelSeed = initializer.createModel(problem, storeBuilder);
		var store = storeBuilder.build();
		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		var initialVersion = model.commit();
		var bestFirst = new BestFirstStoreManager(store, 1);
		bestFirst.startExploration(initialVersion, 0);
		model.restore(bestFirst.getSolutionStore().getSolutions().getFirst().version());
		var serializer = serializerProvider.get();
		var solution = serializer.serializeSolution(initializer.getProblemTrace(), model);
		String actualOutput;
		try (var outputStream = new ByteArrayOutputStream()) {
			solution.eResource().save(outputStream, Map.of());
			actualOutput = outputStream.toString();
		}
		assertThat(actualOutput, is(prefix + "\n" + expectedOutput));
	}

	static Stream<Arguments> solutionSerializerTest() {
		return Stream.of(Arguments.of("""
				class Foo.
				""", """
				scope Foo = 3.
				""", """
				declare foo1, foo2, foo3.
				!exists(Foo::new).
				Foo(foo1).
				Foo(foo2).
				Foo(foo3).
				"""), Arguments.of("""
				class Foo {
					contains Bar[2] bars
				}

				class Bar.
				""", """
				scope Foo = 1.
				""", """
				declare foo1, bar1, bar2.
				!exists(Foo::new).
				!exists(Bar::new).
				Foo(foo1).
				Bar(bar1).
				Bar(bar2).
				bars(foo1, bar1).
				bars(foo1, bar2).
				"""), Arguments.of("""
				class Foo {
					Bar[2] bars opposite foo
				}

				class Bar {
					Foo[1] foo opposite bars
				}
				""", """
				scope Foo = 1, Bar = 2.
				""", """
				declare foo1, bar1, bar2.
				!exists(Foo::new).
				!exists(Bar::new).
				Foo(foo1).
				Bar(bar1).
				Bar(bar2).
				default !bars(*, *).
				bars(foo1, bar1).
				bars(foo1, bar2).
				"""), Arguments.of("""
				class Person {
					Person[2] friend opposite friend
				}
				""", """
				friend(a, b).
				friend(a, c).
				friend(b, c).

				scope Person += 0.
				""", """
				declare a, b, c.
				!exists(Person::new).
				Person(a).
				Person(b).
				Person(c).
				default !friend(*, *).
				friend(a, b).
				friend(a, c).
				friend(b, a).
				friend(b, c).
				friend(c, a).
				friend(c, b).
				"""), Arguments.of("""
				class Foo {
					Bar bar
				}

				enum Bar {
					BAR_A,
					BAR_B
				}
				""", """
				bar(foo, BAR_A).

				scope Foo += 0.
				""", """
				declare foo.
				!exists(Foo::new).
				Foo(foo).
				default !bar(*, *).
				bar(foo, BAR_A).
				"""), Arguments.of("""
				class Foo.
				class Bar extends Foo.
				""", """
				scope Foo = 1, Bar = 0.
				""", """
				declare foo1.
				!exists(Foo::new).
				!exists(Bar::new).
				Foo(foo1).
				!Bar(foo1).
				"""), Arguments.of("""
				class Foo {
					Foo[] ref
				}
				""", """
				ref(a, b).
				!exists(b).

				scope Foo += 0.
				""", """
				declare a.
				!exists(Foo::new).
				Foo(a).
				default !ref(*, *).
				"""), Arguments.of("""
				atom a.
				class Foo.
				""", """
				Foo(a).
				scope Foo += 0.
				""", """
				!exists(Foo::new).
				Foo(a).
				"""), Arguments.of("""
				multi a.
				class Foo.
				""", """
				Foo(a).
				!exists(Foo::new).
				scope Foo = 2.
				""", """
				declare foo1, foo2.
				!exists(a).
				!exists(Foo::new).
				Foo(foo1).
				Foo(foo2).
				"""), Arguments.of("""
				declare a.
				class Foo.
				""", """
				Foo(a).
				?exists(a).
				scope Foo = 2, Foo += 1.
				""", """
				declare foo1.
				!exists(Foo::new).
				Foo(a).
				Foo(foo1).
				"""), Arguments.of("""
				declare a.
				class Foo.
				""", """
				Foo(a).
				?exists(a).
				scope Foo = 1, Foo += 1.
				""", """
				declare foo1.
				!exists(a).
				!exists(Foo::new).
				Foo(foo1).
				"""));
	}
}
