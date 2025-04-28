/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.language.tests.InjectWithRefinery;
import tools.refinery.language.tests.utils.ProblemParseHelper;
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

@InjectWithRefinery
class SolutionSerializerTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Inject
	private Provider<ModelInitializer> initializerProvider;

	@Inject
	private Provider<SolutionSerializer> serializerProvider;

	@ParameterizedTest
	@MethodSource
	void solutionSerializerTest(String prefix, String input, boolean preserveNewNodes,
								String expectedOutput) throws IOException {
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
		String actualOutput;
		try (var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed)) {
			var initialVersion = model.commit();
			var bestFirst = new BestFirstStoreManager(store, 1);
			bestFirst.startExploration(initialVersion, 0);
			model.restore(bestFirst.getSolutionStore().getSolutions().getFirst().version());
			var serializer = serializerProvider.get();
			serializer.setPreserveNewNodes(preserveNewNodes);
			var solution = serializer.serializeSolution(initializer.getProblemTrace(), model);
			try (var outputStream = new ByteArrayOutputStream()) {
				solution.eResource().save(outputStream, Map.of());
				actualOutput = outputStream.toString();
			}
		}
		var normalizedResult = actualOutput.replace("\r\n", "\n");
		var normalizedExpected = (prefix + "\n" + expectedOutput).replace("\r\n", "\n");
		assertThat(normalizedResult, is(normalizedExpected));
	}

	static Stream<Arguments> solutionSerializerTest() {
		return Stream.of(Arguments.of("""
				class Foo.
				""", """
				scope Foo = 3.
				""", false, """
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
				""", false, """
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
				""", false, """
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
				""", false, """
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
				""", false, """
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
				""", false, """
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
				""", false, """
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
				""", false, """
				!exists(Foo::new).
				Foo(a).
				"""), Arguments.of("""
				multi a.
				class Foo.
				""", """
				Foo(a).
				!exists(Foo::new).
				scope Foo = 2.
				""", false, """
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
				""", false, """
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
				""", false, """
				declare foo1.
				!exists(a).
				!exists(Foo::new).
				Foo(foo1).
				"""), Arguments.of("""
				import builtin::strategy.

				class Foo {
					@concretize(false)
					Bar[] bar
				}

				class Bar.
				""", """
				bar(a, b).
				scope Foo = 2, Bar = 2.
				""", false, """
				declare a, b, foo1, bar1.
				!exists(Foo::new).
				!exists(Bar::new).
				Foo(foo1).
				Bar(bar1).
				Foo(a).
				Bar(b).
				default !bar(*, *).
				?bar(foo1, bar1).
				?bar(foo1, b).
				?bar(a, bar1).
				bar(a, b).
				"""), Arguments.of("""
				class Foo.
				class Bar.
				pred bar(Foo x, Bar y).
				""", """
				bar(a, b).
				scope Foo = 2, Bar = 2.
				""", false, """
				declare a, b, foo1, bar1.
				!exists(Foo::new).
				!exists(Bar::new).
				Foo(foo1).
				Bar(bar1).
				Foo(a).
				Bar(b).
				default !bar(*, *).
				bar(a, b).
				"""), Arguments.of("""
				import builtin::strategy.

				class Foo.
				class Bar.

				@concretize(false)
				pred bar(Foo x, Bar y, Bar z).
				""", """
				!bar(*, *, Bar::new).
				bar(a, b, b).
				scope Foo = 2, Bar = 2.
				""", false, """
				declare a, b, foo1, bar1.
				!exists(Foo::new).
				!exists(Bar::new).
				Foo(foo1).
				Bar(bar1).
				Foo(a).
				Bar(b).
				default !bar(*, *, *).
				?bar(foo1, bar1, b).
				?bar(foo1, b, b).
				?bar(a, bar1, b).
				bar(a, b, b).
				"""), Arguments.of("""
				class A {
					B[] foo
				}
				class B.
				""", """
				foo(A::new, B::new).
				foo(A::new, b).
				scope A = 1, B = 1.
				""", true, """
				declare b.
				exists(A::new).
				equals(A::new, A::new).
				!exists(B::new).
				A(A::new).
				B(b).
				default !foo(*, *).
				foo(A::new, b).
				"""), Arguments.of("""
				import builtin::strategy.

				class Foo {
					@concretize(false)
					Bar[] baz

					@concretize(false)
					Bar[] quux
				}

				class Bar.

				pred query(a, b) <-> baz(a, b); quux(a, b).
				""", """
				scope Foo = 1, Bar = 3.
				baz(foo1, bar1).
				query(foo1, bar2).
				Bar(bar3).
				""", false, """
				declare foo1, bar1, bar2, bar3.
				!exists(Foo::new).
				!exists(Bar::new).
				Foo(foo1).
				Bar(bar1).
				Bar(bar2).
				Bar(bar3).
				default !baz(*, *).
				baz(foo1, bar1).
				?baz(foo1, bar2).
				?baz(foo1, bar3).
				default !quux(*, *).
				?quux(foo1, bar1).
				?quux(foo1, bar2).
				?quux(foo1, bar3).
				query(foo1, bar2).
				"""), Arguments.of("""
				import builtin::strategy.

				class Foo {
					@concretize(false)
					Bar[] baz

					@concretize(false)
					Bar[] quux
				}

				class Bar.

				pred query(a, b) <-> baz(a, b), quux(a, b).
				""", """
				scope Foo = 1, Bar = 3.
				Foo(foo1).
				Bar(bar1).
				!baz(foo1, bar1).
				Bar(bar2).
				!query(foo1, bar2).
				Bar(bar3).
				""", false, """
				declare foo1, bar1, bar2, bar3.
				!exists(Foo::new).
				!exists(Bar::new).
				Foo(foo1).
				Bar(bar1).
				Bar(bar2).
				Bar(bar3).
				default !baz(*, *).
				?baz(foo1, bar2).
				?baz(foo1, bar3).
				default !quux(*, *).
				?quux(foo1, bar1).
				?quux(foo1, bar2).
				?quux(foo1, bar3).
				!query(foo1, bar2).
				"""), Arguments.of("""
				class Filesystem {
					contains Dir[1] root
				}

				abstract class FSObject {
					container Dir parent opposite contents
				}

				class Dir extends FSObject {
					contains FSObject[] contents opposite parent
				}

				class File extends FSObject.
				""", """
				Filesystem(git).
				root(git, project).
				contents(project, test).
				Dir(test).
				?exists(test).
				""", false, """
				declare git, project.
				!exists(Filesystem::new).
				!exists(Dir::new).
				!exists(File::new).
				Filesystem(git).
				Dir(project).
				root(git, project).
				"""));
	}
}
