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

import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotEquals
import static org.junit.jupiter.api.Assertions.assertTrue
import tools.refinery.language.mapping.RelationViewConverter
import tools.refinery.language.mapping.RelationViewMappings
import tools.refinery.store.model.representation.Relation
import tools.refinery.language.mapping.PartialModelMapper

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
		assertFalse(relationViewMappings.helperDirectPredicates.containsKey(mayRelView));
		assertFalse(relationViewMappings.helperDirectPredicates.containsKey(mustRelView));
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
		assertTrue(relationViewMappings.helperDirectPredicates.containsKey(mayRelView));
		assertFalse(relationViewMappings.helperDirectPredicates.containsKey(mustRelView));
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
		assertFalse(relationViewMappings.helperDirectPredicates.containsKey(mayRelView));
		assertTrue(relationViewMappings.helperDirectPredicates.containsKey(mustRelView));
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
		assertTrue(relationViewMappings.helperDirectPredicates.containsKey(mayRelView));
		assertTrue(relationViewMappings.helperDirectPredicates.containsKey(mustRelView));
	}
}
