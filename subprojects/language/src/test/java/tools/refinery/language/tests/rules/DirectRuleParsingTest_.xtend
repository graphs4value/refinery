package tools.refinery.language.tests.rules

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import tools.refinery.language.model.problem.Problem
import tools.refinery.language.tests.ProblemInjectorProvider
import tools.refinery.language.model.tests.ProblemTestUtil

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

@ExtendWith(InjectionExtension)
@InjectWith(ProblemInjectorProvider)
class DirectRuleParsingTest {
	@Inject
	ParseHelper<Problem> parseHelper

	@Inject
	extension ProblemTestUtil
	
	@Test
	def void relationValueRewriteTest() {
		val it = parseHelper.parse('''
			pred Person(p).
			direct rule r(p1): Person(p1) = true ~> Person(p1) = false.
		''')
		assertThat(errors, empty)
	}
	
	@Test
	def void relationValueMergeTest() {
		val it = parseHelper.parse('''
			pred Person(p).
			direct rule r(p1): Person(p1): true ~> Person(p1): false.
		''')
		assertThat(errors, empty)
	}
	
	@Test
	def void newNodeTest() {
		val it = parseHelper.parse('''
			pred Person(p).
			direct rule r(p1): Person(p1) = true ~> new p2, Person(p2) = unknown.
		''')
		assertThat(errors, empty)
		assertThat(rule("r").param(0), equalTo(rule("r").conj(0).lit(0).valueAtom.arg(0).variable))
		assertThat(rule("r").actionLit(0).newVar,
			equalTo(rule("r").actionLit(1).valueAtom.arg(0).variable)
		)
	}
	
	@Test
	def void differentScopeTest() {
		val it = parseHelper.parse('''
			pred Friend(a, b).
			direct rule r(p1): Friend(p1, p2) = false ~> new p2, Friend(p1, p2) = true.
		''')
		assertThat(errors, empty)
		assertThat(rule("r").conj(0).lit(0).valueAtom.arg(1).variable,
			not(equalTo(rule("r").actionLit(1).valueAtom.arg(1).variable)))
	}
	
	@Test
	def void parameterShadowingTest() {
		val it = parseHelper.parse('''
			pred Friend(a, b).
			direct rule r(p1, p2): Friend(p1, p2) = false ~> new p2, Friend(p1, p2) = true.
		''')
		assertThat(errors, empty)
		assertThat(rule("r").param(1),
			not(equalTo(rule("r").actionLit(1).valueAtom.arg(1).variable)))
	}
	
	@Test
	def void deleteParameterNodeTest() {
		val it = parseHelper.parse('''
			pred Person(p).
			direct rule r(p1): Person(p1): false ~> delete p1.
		''')
		assertThat(errors, empty)
	}
	
	@Test
	def void deleteDifferentScopeNodeTest() {
		val it = parseHelper.parse('''
			pred Friend(p).
			direct rule r(p1): Friend(p1, p2) = true ~> delete p2.
		''')
		assertThat(errors, not(empty))
	}
	
}
