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
class MultiplicityValidationTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@ValueSource(strings = {"2..5", "2..2", "2..*", "2", ""})
	void validReferenceMultiplicityTest(String range) {
		var problem = parseHelper.parse("""
				class Foo {
					Bar[%s] bar
				}

				class Bar.
				""".formatted(range));
		assertThat(problem.validate(), empty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"2..5", "2..2", "2..*", "2", "0..0", "0"})
	void validScopeMultiplicityTest(String range) {
		var problem = parseHelper.parse("""
				class Foo.

				scope Foo = %s.
				""".formatted(range));
		assertThat(problem.validate(), empty());
	}


	@ParameterizedTest
	@ValueSource(strings = {"0", "0..0"})
	void zeroMReferenceMultiplicityTest(String range) {
		var problem = parseHelper.parse("""
				class Foo {
					Bar[%s] bar
				}

				class Bar.
				""".formatted(range));
		assertThat(problem.validate(), hasItem(allOf(
				hasProperty("severity", is(Diagnostic.WARNING)),
				hasProperty("issueCode", is(ProblemValidator.ZERO_MULTIPLICITY_ISSUE))
		)));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"container Bar bar opposite foo",
			"container Bar[0..1] bar opposite foo",
			"container Bar bar", // Invalid, but has valid multiplicity.
			"container Bar[0..1] bar", // Invalid, but has valid multiplicity.
			"Bar bar opposite foo",
			"Bar[0..1] bar opposite foo"
	})
	void validContainerReference(String referenceText) {
		var problem = parseHelper.parse("""
				class Foo {
					%s
				}

				class Bar {
					contains Foo foo opposite bar
				}
				""".formatted(referenceText));
		assertThat(problem.validate(), not(hasItem(hasProperty("issueCode",
				is(ProblemValidator.INVALID_MULTIPLICITY_ISSUE)))));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"container Bar[1] bar opposite foo",
			"container Bar[1..2] bar opposite foo",
			"container Bar[] bar opposite foo",
			"container Bar[1] bar", // Also otherwise invalid, because the {@code opposite} is missing.
			"container Bar[1..2] bar", // Also otherwise invalid, because the {@code opposite} is missing.
			"container Bar[] bar", // Also otherwise invalid, because the {@code opposite} is missing.
			"Bar[1] bar opposite foo",
			"Bar[1..2] bar opposite foo",
			"Bar[] bar opposite foo"
	})
	void invalidContainerReference(String referenceText) {
		var problem = parseHelper.parse("""
				class Foo {
					%s
				}

				class Bar {
					contains Foo foo opposite bar
				}
				""".formatted(referenceText));
		assertThat(problem.validate(), hasItem(allOf(
				hasProperty("severity", is(Diagnostic.ERROR)),
				hasProperty("issueCode", is(ProblemValidator.INVALID_MULTIPLICITY_ISSUE))
		)));
	}
}
