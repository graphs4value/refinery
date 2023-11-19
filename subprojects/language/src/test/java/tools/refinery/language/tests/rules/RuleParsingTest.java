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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.tests.ProblemInjectorProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Disabled("TODO: Rework transformation rules")
@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class RuleParsingTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@ValueSource(strings = { """
			pred Person(p).
			rule r(p1): must Person(p1) ==> Person(p1) := false.
			""", """
			pred Person(p).
			rule r(p1): must Person(p1) ==> Person(p1) <: false.
			""", """
			pred Person(p).
			rule r(p1): must Person(p1) ==> !Person(p1).
			""", """
			pred Person(p).
			rule r(p1): must Person(p1) ==> delete p1.
			""" })
	void simpleTest(String text) {
		var problem = parseHelper.parse(text);
		assertThat(problem.getResourceErrors(), empty());
	}

	@Test
	void newNodeTest() {
		var problem = parseHelper.parse("""
				pred Person(p).
				rule r(p1): must Person(p1) ==> new p2, Person(p2) := unknown.
				""");
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.rule("r").param(0), equalTo(problem.rule("r").conj(0).lit(0).arg(0).variable()));
		assertThat(problem.rule("r").consequent(0).action(0).newVar(),
				equalTo(problem.rule("r").consequent(0).action(1).assertedAtom().arg(0).variable()));
	}

	@Test
	void differentScopeTest() {
		var problem = parseHelper.parse("""
				pred Friend(a, b).
				rule r(p1): !may Friend(p1, p2) ==> new p2, Friend(p1, p2) := true.
				""");
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.rule("r").conj(0).lit(0).negated().arg(1).variable(),
				not(equalTo(problem.rule("r").consequent(0).action(1).assertedAtom().arg(1).variable())));
	}

	@Test
	void parameterShadowingTest() {
		var problem = parseHelper.parse("""
				pred Friend(a, b).
				rule r(p1, p2): !may Friend(p1, p2) ==> new p2, Friend(p1, p2) := true.
				""");
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.rule("r").param(1),
				not(equalTo(problem.rule("r").consequent(0).action(1).assertedAtom().arg(1).variable())));
	}

	@Test
	void deleteDifferentScopeNodeTest() {
		var problem = parseHelper.parse("""
				pred Person(p).
				rule r(p1): must Friend(p1, p2) ==> delete p2.
				""");
		assertThat(problem.getResourceErrors(), not(empty()));
	}
}
