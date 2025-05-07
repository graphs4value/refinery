/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.validation;

import com.google.inject.Inject;
import org.eclipse.emf.common.util.Diagnostic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.language.annotations.DeclarativeAnnotationValidator;
import tools.refinery.language.tests.InjectWithRefinery;
import tools.refinery.language.tests.utils.ProblemParseHelper;
import tools.refinery.language.validation.ProblemValidator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@InjectWithRefinery
class SupertypeValidationTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@ValueSource(strings = {"""
			class Foo.

			class Bar extends Foo.
			""", """
			class Foo extends node.
			"""})
	void validSupertypeTest(String validText) {
		var problem = parseHelper.parse(validText);
		var issues = problem.validate();
		assertThat(issues, not(hasItem(hasProperty("issueCode", is(ProblemValidator.INVALID_SUPERTYPE_ISSUE)))));
	}

	@ParameterizedTest
	@ValueSource(strings = {"""
			pred foo(node a) <-> true.

			class Bar extends foo.
			""", """
			class Foo extends contained.
			""", """
			class Foo extends container.
			"""})
	void invalidSupertypeTest(String invalidText) {
		var problem = parseHelper.parse(invalidText);
		var issues = problem.validate();
		assertThat(issues, hasItem(hasProperty("issueCode", is(ProblemValidator.INVALID_SUPERTYPE_ISSUE))));
	}

	@Test
	void circularSupertypeTest() {
		var problem = parseHelper.parse("""
				class Foo extends Bar, Baz.

				class Bar extends Quux.

				class Baz.

				class Quux extends Foo, Baz.
				""");
		var issues = problem.validate();
		assertThat(issues, hasItems(
				allOf(
						hasProperty("issueCode", is(ProblemValidator.INVALID_SUPERTYPE_ISSUE)),
						hasProperty("message", containsString("between 'Foo' and 'Bar'"))
				),
				allOf(
						hasProperty("issueCode", is(ProblemValidator.INVALID_SUPERTYPE_ISSUE)),
						hasProperty("message", containsString("between 'Bar' and 'Quux'"))
				),
				allOf(
						hasProperty("issueCode", is(ProblemValidator.INVALID_SUPERTYPE_ISSUE)),
						hasProperty("message", containsString("between 'Quux' and 'Foo'"))
				)
		));
	}

	@ParameterizedTest
	@ValueSource(strings = {"""
			@decide(false) class A.
			""", """
			class A.
			@decide(false) class B extends A.
			"""})
	void validClassDecisionTest(String invalidText) {
		var problem = parseHelper.parse("""
				import builtin::strategy.

				%s
				""".formatted(invalidText));
		var issues = problem.validate();
		assertThat(issues, not(hasItem(
				hasProperty("issueCode", is(DeclarativeAnnotationValidator.ANNOTATION_ISSUE))
		)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"""
			@decide(false) class A.
			@decide class B extends A.
			""", """
			@decide(false) class A.
			@decide(true) class B extends A.
			""", """
			@decide(false) class A.
			class B extends A.
			@decide class C extends B.
			""", """
			@decide(false) class A.
			class B.
			@decide class C extends A, B.
			""", """
			@decide(false) class A extends B.
			@decide class B extends A.
			"""})
	void invalidClassDecisionTest(String invalidText) {
		var problem = parseHelper.parse("""
				import builtin::strategy.

				%s
				""".formatted(invalidText));
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("issueCode", is(DeclarativeAnnotationValidator.ANNOTATION_ISSUE)),
				hasProperty("severity", is(Diagnostic.ERROR))
		)));
		assertThat(issues, not(hasItem(allOf(
				hasProperty("issueCode", is(DeclarativeAnnotationValidator.ANNOTATION_ISSUE)),
				hasProperty("severity", is(Diagnostic.WARNING))
		))));
	}

	@ParameterizedTest
	@ValueSource(strings = {"""
			@decide class A.
			""", """
			@decide(true) class A.
			""", """
			@decide(false) class A.
			@decide(false) class B extends A.
			""", """
			@decide(false) class A.
			class B extends A.
			@decide(false) class C extends B.
			""", """
			@decide(false) class A.
			class B.
			@decide(false) class C extends A, B.
			""", """
			@decide(false) class A extends B.
			@decide(false) class B extends A.
			"""})
	void warnClassDecisionTest(String invalidText) {
		var problem = parseHelper.parse("""
				import builtin::strategy.

				%s
				""".formatted(invalidText));
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("issueCode", is(DeclarativeAnnotationValidator.ANNOTATION_ISSUE)),
				hasProperty("severity", is(Diagnostic.WARNING))
		)));
		assertThat(issues, not(hasItem(allOf(
				hasProperty("issueCode", is(DeclarativeAnnotationValidator.ANNOTATION_ISSUE)),
				hasProperty("severity", is(Diagnostic.ERROR))
		))));
	}
}
