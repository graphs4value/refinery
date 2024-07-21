/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.linking;


import com.google.inject.Inject;
import org.eclipse.xtext.diagnostics.Diagnostic;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.language.tests.InjectWithRefinery;
import tools.refinery.language.tests.utils.ProblemParseHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@InjectWithRefinery
class AmbiguousReferenceTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@ValueSource(strings = {"""
			class Foo {
				contains Quux quux
			}

			class Quux.

			quux(f, q).
			""", """
			class Foo {
				contains Quux quux
			}

			class Quux.

			pred example(Foo f, Quux q) <-> quux(f, q).
			""", """
			class Foo {
				contains Quux quux opposite foo
			}

			class Bar {
				contains Quux quux opposite bar
			}

			class Quux {
				container Foo foo opposite quux
				container Bar bar opposite quux
			}
			"""})
	void unambiguousReferenceTest(String text) {
		var problem = parseHelper.parse(text);
		assertThat(problem.getResourceErrors(), empty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"""
			class Foo {
				contains Quux quux
			}

			class Bar {
				contains Quux quux
			}

			class Quux.

			quux(f, q).
			""", """
			class Foo {
				contains Quux quux
			}

			class Bar {
				contains Quux quux
			}

			class Quux.

			pred example(Foo f, Quuq q) <-> quux(f, q).
			"""})
	void ambiguousReferenceTest(String text) {
		var problem = parseHelper.parse(text);
		var errors = problem.getResourceErrors();
		assertThat(problem.getResourceErrors(), hasItem(allOf(
				hasProperty("code", is(Diagnostic.LINKING_DIAGNOSTIC)),
				hasProperty("message", containsString("'quux'"))
		)));
	}
}
