/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.validation;

import com.google.inject.Inject;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.tests.ProblemInjectorProvider;
import tools.refinery.language.validation.ProblemValidator;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class ArityValidationTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@ValueSource(strings = {"""
			pred Foo(node n) <-> false.
			""", """
			pred Foo(node n, node m) <-> false.
			""", """
			enum Foo { FOO_A, FOO_B }
			"""})
	void invalidSupertypeTest(String supertypeDefinition) {
		var problem = parseHelper.parse("""
				%s

				class Bar extends Foo.
				""".formatted(supertypeDefinition));
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.INVALID_SUPERTYPE_ISSUE)),
				hasProperty("message", stringContainsInOrder("Foo", "Bar"))
		)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"""
			class Foo.
			""", """
			abstract class Foo.
			"""})
	void validSupertypeTest(String supertypeDefinition) {
		var problem = parseHelper.parse("""
				%s

				class Bar extends Foo.
				""".formatted(supertypeDefinition));
		var issues = problem.validate();
		assertThat(issues, empty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"""
			foo().
			""", """
			foo(a1, a2, a3).
			""", """
			pred bar() <-> foo().
			""", """
			pred bar(node n) <-> foo(n, n, n).
			""", """
			pred bar(foo n) <-> false.
			""", """
			scope foo = 1..10.
			""", """
			class Bar {
				foo[] f
			}
			""", """
			class Bar {
				refers foo[] f
			}
			"""})
	void invalidArityTest(String usage) {
		var problem = parseHelper.parse("""
				pred foo(node a, node b) <-> a != b.

				%s
				""".formatted(usage));
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.INVALID_ARITY_ISSUE)),
				hasProperty("message", containsString("foo"))
		)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"""
			foo(a).
			""", """
			pred bar(node m) <-> !foo(m).
			""", """
			pred bar(foo f) <-> true.
			""", """
			scope foo = 1..10.
			""", """
			class Bar {
				foo[] quux
			}
			""", """
			class Bar {
				refers foo[] quux
			}
			"""})
	void validUnaryArityTest(String supertypeDefinition) {
		var problem = parseHelper.parse("""
				pred foo(node n) <-> false.

				%s
				""".formatted(supertypeDefinition));
		var issues = problem.validate();
		assertThat(issues, empty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"""
			foo(a, b).
			""", """
			pred bar(node m) <-> !foo(m, m).
			""", /* Also test for parameters without any type annotation. */ """
			pred bar(m) <-> foo(m, m).
			"""})
	void validBinaryArityTest(String supertypeDefinition) {
		var problem = parseHelper.parse("""
				pred foo(node n, node m) <-> false.

				%s
				""".formatted(supertypeDefinition));
		var issues = problem.validate();
		assertThat(issues, empty());
	}

	@Test
	void notResolvedArityTest() {
		var problem = parseHelper.parse("""
				notResolved(a, b).
				""");
		var issues = problem.validate();
		assertThat(issues, not(contains(hasProperty("issueCode", is(ProblemValidator.INVALID_ARITY_ISSUE)))));
	}

	@Test
	void validTransitiveClosure() {
		var problem = parseHelper.parse("""
				pred foo(node a, node b) <-> false.

				pred bar(a, b) <-> foo+(a, b).
				""");
		var issues = problem.validate();
		assertThat(issues, not(contains(hasProperty("issueCode",
				is(ProblemValidator.INVALID_TRANSITIVE_CLOSURE_ISSUE)))));
	}

	@Test
	void invalidTransitiveClosure() {
		// 0 and 1 argument transitive closures do not get parsed as transitive closure
		// due to the ambiguity with the addition operator {@code a + (b)}.
		var problem = parseHelper.parse("""
				pred foo(node a, node b) <-> false.

				pred bar(node a, node b) <-> foo+(a, b, a).
				""");
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.INVALID_TRANSITIVE_CLOSURE_ISSUE))
		)));
	}

	@ParameterizedTest
	@MethodSource
	void invalidReferenceTypeTest(String definition, String referenceKind) {
		var problem = parseHelper.parse("""
				%s

				class Bar {
					%s Foo foo
				}
				""".formatted(definition, referenceKind));
		var issues = problem.validate();
		assertThat(issues, allOf(
				hasItem(allOf(
						hasProperty("severity", is(Diagnostic.ERROR)),
						hasProperty("issueCode", is(ProblemValidator.INVALID_REFERENCE_TYPE_ISSUE)),
						hasProperty("message", stringContainsInOrder("Foo", "foo"))
				)),
				not(hasItem(hasProperty("issueCode", is(ProblemValidator.INVALID_ARITY_ISSUE))))
		));
	}

	static Stream<Arguments> invalidReferenceTypeTest() {
		return Stream.of(
				"pred Foo(node n) <-> true.",
				"pred Foo(node n, node m) <-> true.",
				"enum Foo { FOO_A, FOO_B }"
		).flatMap(definition -> Stream.of(
				Arguments.of(definition, "contains"),
				Arguments.of(definition, "container")
		));
	}


	@ParameterizedTest
	@MethodSource
	void validReferenceTypeTest(String definition, String referenceKind) {
		var problem = parseHelper.parse("""
				%s

				class Bar {
					%s Foo foo
				}
				""".formatted(definition, referenceKind));
		var issues = problem.validate();
		assertThat(issues, not(hasItem(hasProperty("issueCode", anyOf(
				is(ProblemValidator.INVALID_REFERENCE_TYPE_ISSUE),
				is(ProblemValidator.INVALID_ARITY_ISSUE)
		)))));
	}

	static Stream<Arguments> validReferenceTypeTest() {
		return Stream.of(
				"class Foo.",
				"abstract class Foo."
		).flatMap(definition -> Stream.of(
				Arguments.of(definition, "contains"),
				Arguments.of(definition, "container")
		));
	}
}
