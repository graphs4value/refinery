/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.validation;

import com.google.inject.Inject;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.tests.ProblemInjectorProvider;
import tools.refinery.language.validation.ProblemValidator;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class OppositeValidationTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@ValueSource(strings = {"""
			class Foo {
				Bar bar opposite foo
			}

			class Bar {
				Foo foo opposite bar
			}
			""", """
			class Foo {
				contains Bar bar opposite foo
			}

			class Bar {
				Foo foo opposite bar
			}
			""", """
			class Foo {
				contains Bar bar opposite foo
			}

			class Bar {
				container Foo foo opposite bar
			}
			""", """
			class Foo {
				Foo foo[] opposite foo
			}
			"""})
	void validOppositeTest(String text) {
		var problem = parseHelper.parse(text);
		var issues = problem.validate();
		assertThat(issues, not(hasItems(hasProperty("issueCode", in(Set.of(
				ProblemValidator.INVALID_OPPOSITE_ISSUE,
				ProblemValidator.MISSING_OPPOSITE_ISSUE
		))))));
	}

	@Test
	void missingOppositeTest() {
		var problem = parseHelper.parse("""
				class Foo {
					Bar bar opposite foo
				}

				class Bar {
					Foo foo
				}
				""");
		var issues = problem.validate();
		assertThat(issues, hasItems(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.INVALID_OPPOSITE_ISSUE)),
				hasProperty("message", stringContainsInOrder("foo", "bar"))
		), allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.MISSING_OPPOSITE_ISSUE)),
				hasProperty("message", stringContainsInOrder("bar", "foo"))
		)));
	}

	@Test
	void oppositeMismatchTest() {
		var problem = parseHelper.parse("""
				class Foo {
					Bar bar opposite foo
					Bar quux opposite foo
				}

				class Bar {
					Foo foo opposite bar
				}
				""");
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.INVALID_OPPOSITE_ISSUE)),
				hasProperty("message", stringContainsInOrder("foo", "quux", "bar"))
		)));
	}

	@Test
	void oppositeMismatchProxyTest() {
		var problem = parseHelper.parse("""
				class Foo {
					Bar bar opposite foo
				}

				class Bar {
					Foo foo opposite quux
				}
				""");
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.INVALID_OPPOSITE_ISSUE)),
				hasProperty("message", allOf(
						stringContainsInOrder("foo", "bar"),
						not(containsString("null"))
				))
		)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"contains", "container"})
	void containmentWithProxyOppositeTest(String keyword) {
		var problem = parseHelper.parse("""
				class Foo {
					%s Bar bar opposite foo
				}

				class Bar.
				""".formatted(keyword));
		var issues = problem.validate();
		assertThat(issues, not(hasItem(hasProperty("issueCode",
				is(ProblemValidator.INVALID_OPPOSITE_ISSUE)))));
	}

	@Test
	void containmentWithContainmentOppositeTest() {
		var problem = parseHelper.parse("""
				class Foo {
					contains Bar bar opposite foo
				}

				class Bar {
					contains Foo foo opposite bar
				}
				""");
		var issues = problem.validate();
		assertThat(issues, hasItems(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.INVALID_OPPOSITE_ISSUE)),
				hasProperty("message", stringContainsInOrder("foo", "bar"))
		), allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.INVALID_OPPOSITE_ISSUE)),
				hasProperty("message", stringContainsInOrder("foo", "bar"))
		)));
	}

	@Test
	void containerWithoutOppositeTest() {
		var problem = parseHelper.parse("""
				class Foo {
					container Bar bar
				}

				class Bar.
				""");
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.MISSING_OPPOSITE_ISSUE)),
				hasProperty("message", containsString("bar"))
		)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"Foo foo", "container Foo foo"})
	void containerInvalidOppositeTest(String reference) {
		var problem = parseHelper.parse("""
				class Foo {
					container Bar bar opposite foo
				}

				class Bar {
					%s opposite bar
				}
				""".formatted(reference));
		var issues = problem.validate();
		assertThat(issues, hasItem(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.INVALID_OPPOSITE_ISSUE)),
				hasProperty("message", stringContainsInOrder("foo", "bar"))
		)));
	}
}
