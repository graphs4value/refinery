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
import tools.refinery.language.tests.InjectWithRefinery;
import tools.refinery.language.tests.utils.ProblemParseHelper;
import tools.refinery.language.validation.ProblemValidator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@InjectWithRefinery
class ContainmentValidationTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@ValueSource(strings = {"""
			class A {
				contains b foo
			}

			pred b(node a).
			""", """
			class A {
				contains node foo
			}
			""", """
			class A {
				contains container foo
			}
			"""})
	void invalidReferenceTypeTest(String text) {
		var problem = parseHelper.parse(text);
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("issueCode", is(ProblemValidator.INVALID_REFERENCE_TYPE_ISSUE)),
				hasProperty("severity", is(Diagnostic.ERROR))
		)));
	}

	@Test
	void referenceTypeWarningTest() {
		var problem = parseHelper.parse("""
				class A {
					contains contained foo
				}
				""");
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("issueCode", is(ProblemValidator.INVALID_REFERENCE_TYPE_ISSUE)),
				hasProperty("severity", is(Diagnostic.WARNING))
		)));
	}

	@Test
	void validReferenceTypeTest() {
		var problem = parseHelper.parse("""
				class A {
					contains B foo
				}

				class B.
				""");
		var issues = problem.validate();
		assertThat(issues, not(hasItem(hasProperty("issueCode", is(
				ProblemValidator.INVALID_REFERENCE_TYPE_ISSUE
		)))));
	}
}
