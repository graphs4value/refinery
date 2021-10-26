package tools.refinery.language.mapping.tests

import com.google.inject.Inject
import java.util.HashMap
import java.util.Set
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import tools.refinery.language.mapping.ParsedModelToDNFConverter
import tools.refinery.language.model.problem.Problem
import tools.refinery.language.model.tests.ProblemTestUtil
import tools.refinery.language.tests.ProblemInjectorProvider
import tools.refinery.store.model.representation.TruthValue
import tools.refinery.store.query.building.DNFPredicateCallAtom
import tools.refinery.store.query.building.DirectRelationAtom

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotEquals
import static org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(InjectionExtension)
@InjectWith(ProblemInjectorProvider)
class ParsedModelToDNFConverterTest {
	@Inject
	ParseHelper<Problem> parseHelper

	@Inject
	extension ProblemTestUtil

	ParsedModelToDNFConverter dnfConverter

	@BeforeEach
	def void beforeEach() {
		dnfConverter = new ParsedModelToDNFConverter
	}

	@Test
	def void truthValueTest() {
		val it = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			direct pred p(a) <-> Person(a) = error.
			
		''')
		EcoreUtil.resolveAll(it)

		val pred = pred("p")
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), new HashMap())

		assertTrue(mappings.predicateMap.containsKey(pred))
		val dnfPred = mappings.predicateMap.get(pred)
		assertTrue(mappings.variableMap.containsKey(pred.param(0)))
		val a = mappings.variableMap.get(pred.param(0))
		assertEquals(dnfPred.variables.get(0), a)

		val atom = dnfPred.clauses.get(0).constraints.get(0) as DirectRelationAtom
		assertEquals(atom.substitution.get(0), a)
		assertEquals(atom.allowedTruthValues.get(0), TruthValue.ERROR)
	}

	@Test
	def void refinementTruthValueTest() {
		val it = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			direct pred p(a) <-> Person(a): unknown.
		''')
		EcoreUtil.resolveAll(it)
		val pred = pred("p")
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), new HashMap())

		val dnfPred = mappings.predicateMap.get(pred)
		val a = mappings.variableMap.get(pred.param(0))
		assertEquals(dnfPred.variables.get(0), a)

		val atom = dnfPred.clauses.get(0).constraints.get(0) as DirectRelationAtom
		assertEquals(atom.substitution.get(0), a)
		assertTrue(atom.allowedTruthValues.containsAll(
			Set.of(TruthValue.ERROR, TruthValue.TRUE, TruthValue.FALSE, TruthValue.UNKNOWN)))
	}

	@Test
	def void multipleTruthValueTest() {
		val it = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			direct pred p(a) <-> Person(a) = unknown | true | false.
		''')
		EcoreUtil.resolveAll(it)
		val pred = pred("p")
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), new HashMap())

		val dnfPred = mappings.predicateMap.get(pred)
		val a = mappings.variableMap.get(pred.param(0))
		assertEquals(dnfPred.variables.get(0), a)

		val atom = dnfPred.clauses.get(0).constraints.get(0) as DirectRelationAtom
		assertEquals(atom.substitution.get(0), a)
		assertTrue(atom.allowedTruthValues.containsAll(Set.of(TruthValue.TRUE, TruthValue.FALSE, TruthValue.UNKNOWN)))
	}

	@Test
	def void disjunctionTestTest() {
		val it = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			direct pred p(a, b) <-> Person(a) = true, Person(b) = false ; Person(a) = false, Person(b) = true.
		''')
		EcoreUtil.resolveAll(it)
		val pred = pred("p")
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), new HashMap())

		val dnfPred = mappings.predicateMap.get(pred)
		val a = mappings.variableMap.get(pred.param(0))
		val b = mappings.variableMap.get(pred.param(1))
		assertEquals(dnfPred.clauses.size, 2)
		assertEquals(dnfPred.clauses.get(0).constraints.size, 2)
		assertEquals(dnfPred.clauses.get(1).constraints.size, 2)
		val atom1 = dnfPred.clauses.get(0).constraints.get(0) as DirectRelationAtom
		val atom2 = dnfPred.clauses.get(0).constraints.get(1) as DirectRelationAtom
		val atom3 = dnfPred.clauses.get(1).constraints.get(0) as DirectRelationAtom
		val atom4 = dnfPred.clauses.get(1).constraints.get(1) as DirectRelationAtom
		assertEquals(atom1.substitution.get(0), a)
		assertEquals(atom2.substitution.get(0), b)
		assertEquals(atom3.substitution.get(0), a)
		assertEquals(atom4.substitution.get(0), b)
	}

	@Test
	def void disjunctionWithImplicitVariableTest() {
		val it = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			direct pred p(a) <-> Person(a) = true, Person(b) = false ; Person(a) = false, Person(b) = true.
		''')
		EcoreUtil.resolveAll(it)
		val pred = pred("p")
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), new HashMap())

		val dnfPred = mappings.predicateMap.get(pred)
		val a = mappings.variableMap.get(pred.param(0))
		val b1 = dnfPred.clauses.get(0).existentiallyQuantified.get(0)
		val b2 = dnfPred.clauses.get(1).existentiallyQuantified.get(0)
		assertEquals(mappings.variableMap.get(pred.bodies.get(0).implicitVariables.get(0)), b1)
		assertEquals(mappings.variableMap.get(pred.bodies.get(1).implicitVariables.get(0)), b2)
		assertNotEquals(b1, b2)
		val atom1 = dnfPred.clauses.get(0).constraints.get(0) as DirectRelationAtom
		val atom2 = dnfPred.clauses.get(0).constraints.get(1) as DirectRelationAtom
		val atom3 = dnfPred.clauses.get(1).constraints.get(0) as DirectRelationAtom
		val atom4 = dnfPred.clauses.get(1).constraints.get(1) as DirectRelationAtom
		assertEquals(atom1.substitution.get(0), a)
		assertEquals(atom2.substitution.get(0), b1)
		assertEquals(atom3.substitution.get(0), a)
		assertEquals(atom4.substitution.get(0), b2)
	}

	@Test
	def void predicateUseTest() {
		val it = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			direct pred p1(a) <-> Person(a) = unknown.
			direct pred p2(a, b) <-> p1(a), p1(b), friend(a, b) = true.
		''')
		EcoreUtil.resolveAll(it)
		
		val pred1 = pred("p1")
		val pred2 = pred("p2")
		val mappings = dnfConverter.transformPred(Set.of(pred1, pred2), new HashMap(), new HashMap())

		val dnfPred1 = mappings.predicateMap.get(pred1)
		val dnfPred2 = mappings.predicateMap.get(pred2)
		
		val a1 = mappings.variableMap.get(pred1.param(0))
		val a2 = mappings.variableMap.get(pred2.param(0))
		val b = mappings.variableMap.get(pred2.param(1))
		assertEquals(dnfPred1.variables.get(0), a1)
		assertEquals(dnfPred2.variables.get(0), a2)
		assertNotEquals(a1, a2)
		
		val atom1_1 = dnfPred1.clauses.get(0).constraints.get(0) as DirectRelationAtom
		val atom2_1 = dnfPred2.clauses.get(0).constraints.get(0) as DNFPredicateCallAtom
		val atom2_2 = dnfPred2.clauses.get(0).constraints.get(1) as DNFPredicateCallAtom
		val atom2_3 = dnfPred2.clauses.get(0).constraints.get(2) as DirectRelationAtom
		assertEquals(atom1_1.substitution.get(0), a1)
		assertEquals(atom2_1.substitution.get(0), a2)
		assertEquals(atom2_2.substitution.get(0), b)
		assertEquals(atom2_3.substitution.get(0), a2)
		assertEquals(atom2_3.substitution.get(1), b)
		
		assertEquals(atom2_1.referred, dnfPred1)
		assertEquals(atom2_1.referred, atom2_2.referred)
	}

	@Test
	def void negatedPredicateUseTest() {
		val it = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			direct pred p1(a) <-> Person(a) = unknown.
			direct pred p2(a, b) <-> p1(a), !p1(b).
		''')
		EcoreUtil.resolveAll(it)
		val pred1 = pred("p1")
		val pred2 = pred("p2")
		val mappings = dnfConverter.transformPred(Set.of(pred1, pred2), new HashMap(), new HashMap())

		val dnfPred2 = mappings.predicateMap.get(pred2)
		
		val atom2_1 = dnfPred2.clauses.get(0).constraints.get(0) as DNFPredicateCallAtom
		val atom2_2 = dnfPred2.clauses.get(0).constraints.get(1) as DNFPredicateCallAtom
		
		assertEquals(atom2_1.positive, true)
		assertEquals(atom2_2.positive, false)
	}

	/*@Test
	def void nodesInPredicateTest() {
		val it = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			direct pred p(a) <-> Person(a) = true, friend(a, 'anne') = true, Person(b) = true, friend(b, 'bob') = true.
		''')
		EcoreUtil.resolveAll(it)
	}
	*/
}
