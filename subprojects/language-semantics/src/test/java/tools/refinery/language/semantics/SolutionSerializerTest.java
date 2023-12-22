/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.generator.ModelGeneratorFactory;
import tools.refinery.generator.ProblemLoader;
import tools.refinery.language.tests.ProblemInjectorProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class SolutionSerializerTest {
	@Inject
	ProblemLoader loader;

	@Inject
	ModelGeneratorFactory generatorFactory;

	@Inject
	SolutionSerializer serializer;

	@ParameterizedTest
	@MethodSource
	void solutionSerializerTest(String prefix, String input, String expectedOutput) throws IOException {
		var problem = loader.loadString(prefix + "\n" + input);
		var generator = generatorFactory.createGenerator(problem);
		generator.generate();
		var solution = serializer.serializeSolution(generator.getProblemTrace(), generator.getModel());
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
				!exists(Foo::new).
				Foo(a).
				default !ref(*, *).
				"""));
	}
}
