/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.rules;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.tests.ProblemInjectorProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

@Disabled("TODO: Rework transformation rules")
@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class RuleParsingTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@ValueSource(strings = { """
			pred Person(p).
			rule r(p1): must Person(p1) ==> Person(p1): false.
			""", """
			pred Person(p).
			rule r(p1): must Person(p1) ==> !Person(p1).
			""" })
	void simpleTest(String text) {
		var problem = parseHelper.parse(text);
		assertThat(problem.getResourceErrors(), empty());
	}
}
