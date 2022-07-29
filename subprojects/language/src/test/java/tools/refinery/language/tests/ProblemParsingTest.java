package tools.refinery.language.tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.inject.Inject;

import tools.refinery.language.model.tests.utils.ProblemParseHelper;

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
					int age
					TaxStatus taxStatus
				}

				enum TaxStatus {
					child, student, adult, retired
				}

				% A child cannot have any dependents.
				error invalidTaxStatus(Person p) <->
					taxStatus(p, child), children(p, _q).

				indiv family.
				Family(family).
				members(family, anne): true.
				members(family, bob).
				members(family, ciri).
				children(anne, ciri).
				?children(bob, ciri).
				taxStatus(anne, adult).
				age(anne, 35).
				bobAge: 27.
				age(bob, bobAge).
				!age(ciri, bobAge).
				""");
		assertThat(problem.errors(), empty());
	}
}
