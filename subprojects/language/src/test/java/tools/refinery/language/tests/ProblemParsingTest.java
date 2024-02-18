/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class ProblemParsingTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Test
	void exampleTest() {
		var problem = parseHelper.parse("""
				class Family {
					contains Person[] members
				}

				class Person {
					Person[0..*] children opposite parent
					Person[0..1] parent opposite children
					TaxStatus taxStatus
				}

				enum TaxStatus {
					CHILD, STUDENT, ADULT, RETIRED
				}

				% A child cannot have any dependents.
				error invalidTaxStatus(Person p) <->
					taxStatus(p, CHILD), children(p, _q).

				atom family.
				Family(family).
				members(family, anne): true.
				members(family, bob).
				members(family, ciri).
				children(anne, ciri).
				?children(bob, ciri).
				taxStatus(anne, ADULT).
				""");
		assertThat(problem.getResourceErrors(), empty());
	}
}
