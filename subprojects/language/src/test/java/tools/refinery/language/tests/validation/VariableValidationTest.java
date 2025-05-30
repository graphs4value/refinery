/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
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
class VariableValidationTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Test
	void shouldBeSingletonVariableTest() {
		var problem = parseHelper.parse("""
				pred foo(a, b).

				pred bar(a) <-> foo(a, shouldBeSingleton).
				""");
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("severity", is(Diagnostic.WARNING)),
				hasProperty("issueCode", is(ProblemValidator.SINGLETON_VARIABLE_ISSUE)),
				hasProperty("message", containsString("shouldBeSingleton"))
		)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"_shouldBeSingleton", "_"})
	void singletonVariableTest(String variableName) {
		var problem = parseHelper.parse("""
				pred foo(a, b).

				pred bar(a) <-> foo(a, %s).
				""".formatted(variableName));
		var issues = problem.validate();
		assertThat(issues, not(hasItem(
				hasProperty("issueCode", is(ProblemValidator.SINGLETON_VARIABLE_ISSUE)))));
	}

	@Test
	void shouldBeAtomNodeTest() {
		var problem = parseHelper.parse("""
				class shouldBeAtom.

				pred foo(a, b).

				pred bar(a) <-> foo(a, shouldBeAtom::new).
				""");
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.NODE_CONSTANT_ISSUE)),
				hasProperty("message", containsString("new"))
		)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"node(shouldBeAtom).", "declare shouldBeAtom.", "multi shouldBeAtom."})
	void shouldBeAtomNodeRuleTest(String declaration) {
		var problem = parseHelper.parse("""
				pred foo(a, b).

				rule bar(a) ==> foo(a, shouldBeAtom).

				%s
				""".formatted(declaration));
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.NODE_CONSTANT_ISSUE)),
				hasProperty("message", containsString("shouldBeAtom"))
		)));
	}
}
