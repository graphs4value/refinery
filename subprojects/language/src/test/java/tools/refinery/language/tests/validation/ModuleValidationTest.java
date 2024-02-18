/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.validation;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.tests.ProblemInjectorProvider;
import tools.refinery.language.validation.ProblemValidator;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class ModuleValidationTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@MethodSource
	void invalidMultiObjectExistsTest(String invalidDeclaration) {
		var problem = parseHelper.parse("""
				module.

				%s
				""".formatted(invalidDeclaration));
		var issues = problem.validate();
		assertThat(issues, hasItem(hasProperty("issueCode",
				is(ProblemValidator.UNSUPPORTED_ASSERTION_ISSUE))));
	}

	static Stream<Arguments> invalidMultiObjectExistsTest() {
		return Stream.of(
				Arguments.of("""
						class Foo.
						exists(Foo::new).
						"""),
				Arguments.of("""
						multi m.
						exists(m).
						"""));
	}

	@Test
	void invalidScopeTest() {
		var problem = parseHelper.parse("""
				module.

				class Foo.
				scope Foo += 1.
				""");
		var issues = problem.validate();
		assertThat(issues, hasItem(hasProperty("issueCode",
				is(ProblemValidator.INVALID_MULTIPLICITY_ISSUE))));
	}

	@Test
	void invalidAssertionArgumentTest() {
		var problem = parseHelper.parse("""
				module.

				class Foo.
				Foo(foo1).
				""");
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("issueCode", is(ProblemValidator.UNSUPPORTED_ASSERTION_ISSUE)),
				hasProperty("message", containsString("foo1")))));
	}

	@ParameterizedTest
	@MethodSource
	void validDeclarationTest(String validDeclaration) {
		var problem = parseHelper.parse(validDeclaration);
		var issues = problem.validate();
		assertThat(issues, hasSize(0));
	}

	static Stream<Arguments> validDeclarationTest() {
		return Stream.concat(
				invalidMultiObjectExistsTest(),
				Stream.of(
						Arguments.of("""
								class Foo.
								scope Foo += 1.
								"""),
						Arguments.of("""
								module.

								class Foo.
								scope Foo = 1.
								"""),
						Arguments.of("""
								class Foo.
								Foo(foo1).
								"""),
						Arguments.of("""
								module.

								class Foo.
								multi foo1.
								Foo(foo1).
								"""),
						Arguments.of("""
								module.

								class Foo.
								atom foo1.
								Foo(foo1).
								"""),
						Arguments.of("""
								module.

								class Foo.
								declare foo1.
								Foo(foo1).
								"""),
						Arguments.of("""
								module.

								enum Foo { foo1 }
								Foo(foo1).
								"""),
						Arguments.of("""
								module.

								class Foo.
								Foo(Foo::new).
								""")
				)
		);
	}
}
