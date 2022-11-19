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
					refers Person[0..*] children opposite parent
					refers Person[0..1] parent opposite children
					int age
					refers TaxStatus taxStatus
				}

				enum TaxStatus {
					child, student, adult, retired
				}

				% A child cannot have any dependents.
				error invalidTaxStatus(Person p) <->
					taxStatus(p, child), children(p, _q).

				individual family.
				Family(family).
				members(family, anne): true.
				members(family, bob).
				members(family, ciri).
				children(anne, ciri).
				?children(bob, ciri).
				taxStatus(anne, adult).
				age(bob) in 21..35.
				age(ciri) = 10.
				""");
		assertThat(problem.errors(), empty());
	}
}
