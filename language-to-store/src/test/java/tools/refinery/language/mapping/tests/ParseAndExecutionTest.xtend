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
class ParseAndExecutionTest {
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
	def void parseAndExecutionTest() {
		val it = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			
			friend(a, b).
			friend(a, c).
			friend(a, d): false.
			friend(b, c): false.
			direct pred p(a) <-> Person(a) = true.
		''')
		EcoreUtil.resolveAll(it)
		
		val pred = pred("p")
		val mapper = new PartialModelMapper
		val relationMap = mapper.transformProblem(it).relationMap
		val mappings = dnfConverter.transformPred(Set.of(pred), new HashMap(), relationMap)
		val relationViewConverter = new RelationViewConverter
		val relationViewMappings = relationViewConverter.convertDirectPredicates(mappings.predicateMap.values,
			relationMap.values)

		// relations, relationView, DNFPred lista
		/*
		QueriableModelStore store = new QueriableModelStoreImpl(Set.of(person, friend),
				Set.of(persionView, friendMustView), Set.of(friendPredicate, predicate));
		QueriableModel model = store.createModel();
		*/
		
	}
}
