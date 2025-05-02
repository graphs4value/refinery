/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.validation;

import com.google.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
}
