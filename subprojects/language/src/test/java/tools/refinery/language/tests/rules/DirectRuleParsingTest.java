package tools.refinery.language.tests.rules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.inject.Inject;

import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.tests.ProblemInjectorProvider;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class DirectRuleParsingTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@ValueSource(strings = { """
			pred Person(p).
			direct rule r(p1): Person(p1) = true ~> Person(p1) = false.
			""", """
			pred Person(p).
			direct rule r(p1): Person(p1) = true ~> Person(p1): false.
			""", """
			pred Person(p).
			direct rule r(p1): Person(p1): false ~> delete p1.
			""" })
	void simpleTest(String text) {
		var problem = parseHelper.parse(text);
		assertThat(problem.errors(), empty());
	}

	@Test
	void newNodeTest() {
		var problem = parseHelper.parse("""
				pred Person(p).
				direct rule r(p1): Person(p1) = true ~> new p2, Person(p2) = unknown.
				""");
		assertThat(problem.errors(), empty());
		assertThat(problem.rule("r").param(0), equalTo(problem.rule("r").conj(0).lit(0).valueAtom().arg(0).variable()));
		assertThat(problem.rule("r").actionLit(0).newVar(),
				equalTo(problem.rule("r").actionLit(1).valueAtom().arg(0).variable()));
	}

	@Test
	void differentScopeTest() {
		var problem = parseHelper.parse("""
				pred Friend(a, b).
				direct rule r(p1): Friend(p1, p2) = false ~> new p2, Friend(p1, p2) = true.
				""");
		assertThat(problem.errors(), empty());
		assertThat(problem.rule("r").conj(0).lit(0).valueAtom().arg(1).variable(),
				not(equalTo(problem.rule("r").actionLit(1).valueAtom().arg(1).variable())));
	}

	@Test
	void parameterShadowingTest() {
		var problem = parseHelper.parse("""
				pred Friend(a, b).
				direct rule r(p1, p2): Friend(p1, p2) = false ~> new p2, Friend(p1, p2) = true.
				""");
		assertThat(problem.errors(), empty());
		assertThat(problem.rule("r").param(1),
				not(equalTo(problem.rule("r").actionLit(1).valueAtom().arg(1).variable())));
	}

	@Test
	void deleteDifferentScopeNodeTest() {
		var problem = parseHelper.parse("""
				pred Person(p).
				direct rule r(p1): Friend(p1, p2) = true ~> delete p2.
				""");
		assertThat(problem.errors(), not(empty()));
	}
}
