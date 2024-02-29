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
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.tests.ProblemInjectorProvider;
import tools.refinery.language.validation.ProblemValidator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class AssignmentValidationTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@ValueSource(strings = {"""
			pred foo(node a) <-> 5 is 5.
			""", """
			pred foo(node a) <-> b + 2 is 5.
			""", """
			pred foo(node a) <-> a is 5.
			""", """
			node(n).
			pred foo(node a) <-> n is 5.
			""", """
			enum E { A, B }
			pred foo(node a) <-> B is 5.
			"""})
	void invalidAssignmentTest(String text) {
		var problem = parseHelper.parse(text);
		var issues = problem.validate();
		assertThat(issues, hasItem(hasProperty("issueCode", is(
				ProblemValidator.INVALID_ASSIGNMENT_ISSUE
		))));
	}

	@Test
	void validAssignmentTest() {
		var problem = parseHelper.parse("""
				pred foo(node a) <-> b is 5.
				""");
		var issues = problem.validate();
		assertThat(issues, not(hasItem(hasProperty("issueCode", is(
				ProblemValidator.INVALID_ASSIGNMENT_ISSUE
		)))));
	}
}
