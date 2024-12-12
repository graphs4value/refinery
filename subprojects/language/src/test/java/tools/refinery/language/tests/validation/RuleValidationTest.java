/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
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
import static org.hamcrest.Matchers.containsString;

@InjectWithRefinery
class RuleValidationTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Test
	void diagonalMultiObjectTest() {
		var problem = parseHelper.parse("""
				import builtin::strategy.

				class Foo {
					Foo[] friend
				}

				decision rule notFriend(@multi Foo aParameter) ==> !friend(aParameter, aParameter).
				""");
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("issueCode", is(ProblemValidator.INVALID_MODALITY_ISSUE)),
				hasProperty("message", containsString("aParameter")))));
	}

	@ParameterizedTest
	@ValueSource(strings = {"""
			import builtin::strategy.

			class Foo.

			pred bar(Foo a).

			pred quux(Foo a).

			decision rule notMultiple(@multi Foo a) ==> !bar(a), quux(a).
			""", """
			class Foo {
				Foo[] friend
			}

			decision rule notFriend(@multi Foo a, @multi Foo b) <->
				may equals(a, b)
			==>
				!friend(a, b).
			"""})
	void notDiagonalMultiObjectTest(String validText) {
		var problem = parseHelper.parse(validText);
		var issues = problem.validate();
		assertThat(issues, not(hasItem(
				hasProperty("issueCode", is(ProblemValidator.INVALID_MODALITY_ISSUE)))));
	}
}
