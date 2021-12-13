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
import tools.refinery.language.mapping.PartialModelMapper
import tools.refinery.language.mapping.RelationViewConverter
import tools.refinery.language.model.problem.Problem
import tools.refinery.language.model.tests.ProblemTestUtil
import tools.refinery.language.tests.ProblemInjectorProvider
import tools.refinery.store.query.building.DNFPredicateCallAtom
import tools.refinery.store.query.building.RelationAtom

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertNotEquals
import static org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(InjectionExtension)
@InjectWith(ProblemInjectorProvider)
class RelationViewConverterTest {
	@Inject
	ParseHelper<Problem> parseHelper

	@Inject
	extension ProblemTestUtil

	PartialModelMapper partialModelMapper
	ParsedModelToDNFConverter dnfConverter
	RelationViewConverter relationViewConverter

	@BeforeEach
	def void beforeEach() {
		partialModelMapper = new PartialModelMapper
		dnfConverter = new ParsedModelToDNFConverter
		relationViewConverter = new RelationViewConverter
	}

	@Test
	def void trueRelationViewTest() {
		val it = parseHelper.parse('''
			class Person.
			direct pred p(a) <-> Person(a) = true.
			
		''')
		EcoreUtil.resolveAll(it)

		val pred = pred("p")
		val relationMap = partialModelMapper.transformProblem(it).relationMap
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), relationMap)
		val relationViewMappings = relationViewConverter.convertDirectPredicates(mappings.predicateMap.values,
			relationMap.values)

		assertNotEquals(relationViewMappings.directPredicates, mappings.predicateMap.values)

		val personRel = relationViewMappings.relations.findFirst[name.equals("Person")]
		assertTrue(relationViewMappings.mayViewMap.containsKey(personRel));
		assertTrue(relationViewMappings.mustViewMap.containsKey(personRel));
		val mayRelView = relationViewMappings.mayViewMap.get(personRel);
		val mustRelView = relationViewMappings.mustViewMap.get(personRel);
		assertFalse(relationViewMappings.mayHelperDirectPredicates.containsKey(mayRelView));
		assertFalse(relationViewMappings.mustHelperDirectPredicates.containsKey(mustRelView));

		val dnfAnd = relationViewMappings.directPredicates.get(0).clauses.get(0).constraints
		val mayDNFAtom = dnfAnd.get(0)
		val mustDNFAtom = dnfAnd.get(1)
		assertEquals((mayDNFAtom as RelationAtom).getView, mayRelView)
		assertEquals((mustDNFAtom as RelationAtom).getView, mustRelView)
	}

	@Test
	def void errorRelationViewTest() {
		val it = parseHelper.parse('''
			class Person.
			direct pred p(a) <-> Person(a) = error.
			
		''')
		EcoreUtil.resolveAll(it)

		val pred = pred("p")
		val relationMap = partialModelMapper.transformProblem(it).relationMap
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), relationMap)
		val relationViewMappings = relationViewConverter.convertDirectPredicates(mappings.predicateMap.values,
			relationMap.values)

		val personRel = relationViewMappings.relations.findFirst[name.equals("Person")]
		val mayRelView = relationViewMappings.mayViewMap.get(personRel);
		val mustRelView = relationViewMappings.mustViewMap.get(personRel);
		assertTrue(relationViewMappings.mayHelperDirectPredicates.containsKey(mayRelView));
		assertFalse(relationViewMappings.mustHelperDirectPredicates.containsKey(mustRelView));

		val dnfAnd = relationViewMappings.directPredicates.get(0).clauses.get(0).constraints
		val mayDNFAtom = dnfAnd.get(0)
		val mustDNFAtom = dnfAnd.get(1)
		val mayRelAtom = (mayDNFAtom as DNFPredicateCallAtom).referred.clauses.get(0).constraints.get(0)
		assertEquals((mayRelAtom as RelationAtom).view, mayRelView)
		assertEquals((mustDNFAtom as RelationAtom).view, mustRelView)
	}

	@Test
	def void unknownRelationViewTest() {
		val it = parseHelper.parse('''
			class Person.
			direct pred p(a) <-> Person(a) = unknown.
			
		''')
		EcoreUtil.resolveAll(it)

		val pred = pred("p")
		val relationMap = partialModelMapper.transformProblem(it).relationMap
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), relationMap)
		val relationViewMappings = relationViewConverter.convertDirectPredicates(mappings.predicateMap.values,
			relationMap.values)

		val personRel = relationViewMappings.relations.findFirst[name.equals("Person")]
		val mayRelView = relationViewMappings.mayViewMap.get(personRel);
		val mustRelView = relationViewMappings.mustViewMap.get(personRel);
		assertFalse(relationViewMappings.mayHelperDirectPredicates.containsKey(mayRelView));
		assertTrue(relationViewMappings.mustHelperDirectPredicates.containsKey(mustRelView));

		val dnfAnd = relationViewMappings.directPredicates.get(0).clauses.get(0).constraints
		val mayDNFAtom = dnfAnd.get(0)
		val mustDNFAtom = dnfAnd.get(1)
		val mustRelAtom = (mustDNFAtom as DNFPredicateCallAtom).referred.clauses.get(0).constraints.get(0)
		assertEquals((mayDNFAtom as RelationAtom).view, mayRelView)
		assertEquals((mustRelAtom as RelationAtom).view, mustRelView)
	}

	@Test
	def void falseRelationViewTest() {
		val it = parseHelper.parse('''
			class Person.
			direct pred p(a) <-> Person(a) = false.
			
		''')
		EcoreUtil.resolveAll(it)

		val pred = pred("p")
		val relationMap = partialModelMapper.transformProblem(it).relationMap
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), relationMap)
		val relationViewMappings = relationViewConverter.convertDirectPredicates(mappings.predicateMap.values,
			relationMap.values)

		val personRel = relationViewMappings.relations.findFirst[name.equals("Person")]
		val mayRelView = relationViewMappings.mayViewMap.get(personRel);
		val mustRelView = relationViewMappings.mustViewMap.get(personRel);
		assertTrue(relationViewMappings.mayHelperDirectPredicates.containsKey(mayRelView));
		assertTrue(relationViewMappings.mustHelperDirectPredicates.containsKey(mustRelView));

		val dnfAnd = relationViewMappings.directPredicates.get(0).clauses.get(0).constraints
		val mayDNFAtom = dnfAnd.get(0)
		val mustDNFAtom = dnfAnd.get(1)
		val mayRelAtom = (mayDNFAtom as DNFPredicateCallAtom).referred.clauses.get(0).constraints.get(0)
		val mustRelAtom = (mustDNFAtom as DNFPredicateCallAtom).referred.clauses.get(0).constraints.get(0)
		assertEquals((mayRelAtom as RelationAtom).view, mayRelView)
		assertEquals((mustRelAtom as RelationAtom).view, mustRelView)
	}

	@Test
	def void falseErrorRelationViewTest() {
		val it = parseHelper.parse('''
			class Person.
			direct pred p(a) <-> Person(a) = false | error.
			
		''')
		EcoreUtil.resolveAll(it)

		val pred = pred("p")
		val relationMap = partialModelMapper.transformProblem(it).relationMap
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), relationMap)
		val relationViewMappings = relationViewConverter.convertDirectPredicates(mappings.predicateMap.values,
			relationMap.values)

		val personRel = relationViewMappings.relations.findFirst[name.equals("Person")]
		val mayRelView = relationViewMappings.mayViewMap.get(personRel);
		val mustRelView = relationViewMappings.mustViewMap.get(personRel);
		assertTrue(relationViewMappings.mayHelperDirectPredicates.containsKey(mayRelView));
		assertFalse(relationViewMappings.mustHelperDirectPredicates.containsKey(mustRelView));

		val dnfAnd = relationViewMappings.directPredicates.get(0).clauses.get(0).constraints
		val mayDNFAtom = dnfAnd.get(0)
		val mayRelAtom = (mayDNFAtom as DNFPredicateCallAtom).referred.clauses.get(0).constraints.get(0)
		assertEquals((mayRelAtom as RelationAtom).view, mayRelView)
	}

	@Test
	def void falseUnknownRelationViewTest() {
		val it = parseHelper.parse('''
			class Person.
			direct pred p(a) <-> Person(a) = false | unknown.
			
		''')
		EcoreUtil.resolveAll(it)

		val pred = pred("p")
		val relationMap = partialModelMapper.transformProblem(it).relationMap
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), relationMap)
		val relationViewMappings = relationViewConverter.convertDirectPredicates(mappings.predicateMap.values,
			relationMap.values)

		val personRel = relationViewMappings.relations.findFirst[name.equals("Person")]
		val mayRelView = relationViewMappings.mayViewMap.get(personRel);
		val mustRelView = relationViewMappings.mustViewMap.get(personRel);
		assertFalse(relationViewMappings.mayHelperDirectPredicates.containsKey(mayRelView));
		assertTrue(relationViewMappings.mustHelperDirectPredicates.containsKey(mustRelView));

		val dnfAnd = relationViewMappings.directPredicates.get(0).clauses.get(0).constraints
		val mustDNFAtom = dnfAnd.get(0)
		val mustRelAtom = (mustDNFAtom as DNFPredicateCallAtom).referred.clauses.get(0).constraints.get(0)
		assertEquals((mustRelAtom as RelationAtom).view, mustRelView)
	}

	@Test
	def void trueErrorRelationViewTest() {
		val it = parseHelper.parse('''
			class Person.
			direct pred p(a) <-> Person(a) = true | error.
			
		''')
		EcoreUtil.resolveAll(it)

		val pred = pred("p")
		val relationMap = partialModelMapper.transformProblem(it).relationMap
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), relationMap)
		val relationViewMappings = relationViewConverter.convertDirectPredicates(mappings.predicateMap.values,
			relationMap.values)

		val personRel = relationViewMappings.relations.findFirst[name.equals("Person")]
		val mayRelView = relationViewMappings.mayViewMap.get(personRel);
		val mustRelView = relationViewMappings.mustViewMap.get(personRel);
		assertFalse(relationViewMappings.mayHelperDirectPredicates.containsKey(mayRelView));
		assertFalse(relationViewMappings.mustHelperDirectPredicates.containsKey(mustRelView));

		val dnfAnd = relationViewMappings.directPredicates.get(0).clauses.get(0).constraints
		val mustDNFAtom = dnfAnd.get(0)
		assertEquals((mustDNFAtom as RelationAtom).view, mustRelView)
	}

	@Test
	def void trueUnknownRelationViewTest() {
		val it = parseHelper.parse('''
			class Person.
			direct pred p(a) <-> Person(a) = true | unknown.
			
		''')
		EcoreUtil.resolveAll(it)

		val pred = pred("p")
		val relationMap = partialModelMapper.transformProblem(it).relationMap
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), relationMap)
		val relationViewMappings = relationViewConverter.convertDirectPredicates(mappings.predicateMap.values,
			relationMap.values)

		val personRel = relationViewMappings.relations.findFirst[name.equals("Person")]
		val mayRelView = relationViewMappings.mayViewMap.get(personRel);
		val mustRelView = relationViewMappings.mustViewMap.get(personRel);
		assertFalse(relationViewMappings.mayHelperDirectPredicates.containsKey(mayRelView));
		assertFalse(relationViewMappings.mustHelperDirectPredicates.containsKey(mustRelView));

		val dnfAnd = relationViewMappings.directPredicates.get(0).clauses.get(0).constraints
		val mayDNFAtom = dnfAnd.get(0)
		assertEquals((mayDNFAtom as RelationAtom).view, mayRelView)
	}

	@Test
	def void allTruthValueRelationViewTest() {
		val it = parseHelper.parse('''
			class Person.
			direct pred p(a) <-> Person(a) = true | false | unknown | error.
			
		''')
		EcoreUtil.resolveAll(it)

		val pred = pred("p")
		val relationMap = partialModelMapper.transformProblem(it).relationMap
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), relationMap)
		val relationViewMappings = relationViewConverter.convertDirectPredicates(mappings.predicateMap.values,
			relationMap.values)

		val personRel = relationViewMappings.relations.findFirst[name.equals("Person")]
		val mayRelView = relationViewMappings.mayViewMap.get(personRel);
		val mustRelView = relationViewMappings.mustViewMap.get(personRel);
		assertFalse(relationViewMappings.mayHelperDirectPredicates.containsKey(mayRelView));
		assertFalse(relationViewMappings.mustHelperDirectPredicates.containsKey(mustRelView));

		val dnfAnd = relationViewMappings.directPredicates.get(0).clauses.get(0).constraints
		assertTrue(dnfAnd.empty)
	}

	@Test
	def void directPredicateReferenceTest() {
		val it = parseHelper.parse('''
			class Person.
			direct pred p1(a) <-> Person(a) = true.
			direct pred p2(a) <-> p1(a).
			
		''')
		EcoreUtil.resolveAll(it)

		val relationMap = partialModelMapper.transformProblem(it).relationMap
		val mappings = dnfConverter.transformPred(Set.of(pred("p1"), pred("p2")), new HashMap(), relationMap)
		val dnfPreds = mappings.predicateMap.values

		val p1 = dnfPreds.findFirst[name.equals("p1")]
		val p2 = dnfPreds.findFirst[name.equals("p2")]
		val p1Call = p2.clauses.get(0).constraints.get(0) as DNFPredicateCallAtom
		assertEquals(p1, p1Call.referred)

		val relationViewMappings = relationViewConverter.convertDirectPredicates(dnfPreds, relationMap.values)
		val p1Conv = relationViewMappings.directPredicates.findFirst[name.equals("p1")]
		val p2Conv = relationViewMappings.directPredicates.findFirst[name.equals("p2")]
		val p1ConvCall = p2Conv.clauses.get(0).constraints.get(0) as DNFPredicateCallAtom
		assertEquals(p1Conv, p1ConvCall.referred)
	}
}
