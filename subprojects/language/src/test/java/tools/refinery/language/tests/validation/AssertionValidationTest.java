/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
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
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.tests.ProblemInjectorProvider;
import tools.refinery.language.validation.ProblemValidator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class AssertionValidationTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Test
	void invalidValueTest() {
		var problem = parseHelper.parse("""
				class Foo.

				Foo(n): 5.
				""");
		var issues = problem.validate();
		assertThat(issues, hasItem(hasProperty("issueCode",
				is(ProblemValidator.INVALID_VALUE_ISSUE))));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"default exists(n).",
			"!exists(A).",
			"exists(A): error.",
			"exists(n): error.",
			"!exists(*).",
			"exists(*): error.",
			"default equals(n, n).",
			"equals(n, m).",
			"?equals(n, m).",
			"equals(n, m): error.",
			"equals(A, B).",
			"?equals(A, B).",
			"equals(A, B): error.",
			"!equals(n, n).",
			"equals(n, n): error.",
			"!equals(A, A).",
			"equals(A, A): error.",
			"?equals(n, *).",
			"?equals(*, m).",
			"equals(*, *).",
			"!equals(*, *).",
			"?equals(*, *).",
			"equals(*, *): error."
	})
	void invalidMultiObjectTest(String assertion) {
		var problem = parseHelper.parse("""
				enum Bar { A, B }

				%s
				""".formatted(assertion));
		var issues = problem.validate();
		assertThat(issues, hasItem(hasProperty("issueCode",
				is(ProblemValidator.UNSUPPORTED_ASSERTION_ISSUE))));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"exists(A).",
			"?exists(A).",
			"exists(n).",
			"?exists(n).",
			"!exists(n).",
			"?exists(*).",
			"exists(Foo::new).",
			"?exists(Foo::new).",
			"!exists(Foo::new).",
			"equals(A, A).",
			"?equals(A, A).",
			"!equals(A, B).",
			"equals(n, n).",
			"?equals(n, n).",
			"!equals(n, m).",
			"equals(Foo::new, Foo::new).",
			"?equals(Foo::new, Foo::new)."
	})
	void validMultiObjectTest(String assertion) {
		var problem = parseHelper.parse("""
				class Foo.

				enum Bar { A, B }

				%s
				""".formatted(assertion));
		var issues = problem.validate();
		assertThat(issues, not(hasItem(hasProperty("issueCode",
				is(ProblemValidator.UNSUPPORTED_ASSERTION_ISSUE)))));
	}
}
