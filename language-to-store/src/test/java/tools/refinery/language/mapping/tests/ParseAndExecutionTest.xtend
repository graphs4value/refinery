package tools.refinery.language.mapping.tests

import com.google.inject.Inject
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import tools.refinery.language.mapping.QueryableModelMapper
import tools.refinery.language.model.problem.Problem
import tools.refinery.language.tests.ProblemInjectorProvider
import tools.refinery.store.query.QueriableModel

import static org.junit.jupiter.api.Assertions.assertEquals

@ExtendWith(InjectionExtension)
@InjectWith(ProblemInjectorProvider)
class ParseAndExecutionTest {
	@Inject
	ParseHelper<Problem> parseHelper

	@Test
	def void classRelationTrueTest() {
		val it = parseHelper.parse('''
			class Person.
			
			direct pred p(x) <-> Person(x) = true.
		''')
		EcoreUtil.resolveAll(it)

		val mapper = new QueryableModelMapper
		val partialModelMapperDTO = mapper.transformProblem(it)
		val model = partialModelMapperDTO.getModel as QueriableModel
		val pred = model.predicates.findFirst[name == "p"]
		assertEquals(1, model.countResults(pred));
	}

	@Test
	def void existsWithUnaryRelationTest() {
		val it = parseHelper.parse('''
			class Person.
			
			Person(a).
			Person(b).
			Person(c).
			Person(d).
			
			direct pred p(x) <-> exists(x) = true, Person(x) = true.
		''')
		EcoreUtil.resolveAll(it)

		val mapper = new QueryableModelMapper
		val partialModelMapperDTO = mapper.transformProblem(it)
		val model = partialModelMapperDTO.getModel as QueriableModel
		val pred = model.predicates.findFirst[name == "p"]
		assertEquals(4, model.countResults(pred));
	}

	@Test
	def void unaryRelationTest() {
		val it = parseHelper.parse('''
			class Person.
			
			Person(a): true.
			Person(b): false.
			Person(c): unknown.
			Person(d): error.
			
			direct pred pTrue(x)    <-> exists(x) = true, Person(x) = true.
			direct pred pFalse(x)   <-> exists(x) = true, Person(x) = false.
			direct pred pUnknown(x) <-> exists(x) = true, Person(x) = unknown.
			direct pred pError(x)   <-> exists(x) = true, Person(x) = error.
			direct pred pFE(x)      <-> exists(x) = true, Person(x) = false|error.
			direct pred pFU(x)      <-> exists(x) = true, Person(x) = false|unknown.
			direct pred pTE(x)      <-> exists(x) = true, Person(x) = true|error.
			direct pred pTU(x)      <-> exists(x) = true, Person(x) = true|unknown.
			direct pred pAll(x)     <-> exists(x) = true, Person(x) = true|false|unknown|error.
		''')
		EcoreUtil.resolveAll(it)

		val mapper = new QueryableModelMapper
		val partialModelMapperDTO = mapper.transformProblem(it)
		val model = partialModelMapperDTO.getModel as QueriableModel
		//printmodel(model)
		val pTrue = model.predicates.findFirst[name == "pTrue"]
		val pFalse = model.predicates.findFirst[name == "pFalse"]
		val pUnknown = model.predicates.findFirst[name == "pUnknown"]
		val pError = model.predicates.findFirst[name == "pError"]
		val pFE = model.predicates.findFirst[name == "pFE"]
		val pFU = model.predicates.findFirst[name == "pFU"]
		val pTE = model.predicates.findFirst[name == "pTE"]
		val pTU = model.predicates.findFirst[name == "pTU"]
		val pAll = model.predicates.findFirst[name == "pAll"]
		assertEquals(1, model.countResults(pTrue));
		assertEquals(1, model.countResults(pFalse));
		// assertEquals(1, model.countResults(pUnknown));
		assertEquals(1, model.countResults(pError));
		assertEquals(2, model.countResults(pFE));
		// assertEquals(2, model.countResults(pFU));
		assertEquals(2, model.countResults(pTE));
	// assertEquals(2, model.countResults(pTU));
	// assertEquals(4, model.countResults(pAll));
	}
	
	protected def void printmodel(QueriableModel model) {
		for(rep : model.dataRepresentations) {
			println(rep.name)
			println("-------------")
			val cursor = model.getAll(rep)
			while(cursor.move) {
				println('''«cursor.key» -> «cursor.value»''')
			}
			println
		}
	}

	@Test
	def void equalsRelationTest() {
		val it = parseHelper.parse('''
			class Person.
			
			Person(a).
			Person(b).
			Person(c).
			Person(d).
			
			direct pred p(x) <->
				exists(x) = true, Person(x) = true.
			direct pred p2(x, y) <->
				exists(x) = true, exists(y) = true, Person(x) = true, Person(y) = true.
			direct pred pEq(x, y) <->
				exists(x) = true, exists(y) = true, Person(x) = true, Person(y) = true, equals(x, y) = false.
		''')
		EcoreUtil.resolveAll(it)

		val mapper = new QueryableModelMapper
		val partialModelMapperDTO = mapper.transformProblem(it)
		val model = partialModelMapperDTO.getModel as QueriableModel
		val p = model.predicates.findFirst[name == "p"]
		val p2 = model.predicates.findFirst[name == "p2"]
		val pEq = model.predicates.findFirst[name == "pEq"]
		assertEquals(4, model.countResults(p));
		assertEquals(16, model.countResults(p2));
		assertEquals(12, model.countResults(pEq));
	}

	@Test
	def void binaryRelationTest() {
		val it = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			
			Person(a).
			Person(b).
			Person(c).
			Person(d).
			friend(a, b).
			friend(a, c).
			friend(a, d).
			friend(b, c).
			
			direct pred pDirectedFriends(x, y) <->
				exists(x) = true, exists(y) = true, Person(x) = true, Person(y) = true,
				friend(x, y) = true.
			direct pred pUnknownFriends(x, y) <->
				exists(x) = true, exists(y) = true, Person(x) = true, Person(y) = true,
				friend(x, y) = unknown, equals(x, y) = false.
		''')
		EcoreUtil.resolveAll(it)

		val mapper = new QueryableModelMapper
		val partialModelMapperDTO = mapper.transformProblem(it)
		val model = partialModelMapperDTO.getModel as QueriableModel
		val p1 = model.predicates.findFirst[name == "pDirectedFriends"]
		val p2 = model.predicates.findFirst[name == "pUnknownFriends"]
		assertEquals(4, model.countResults(p1));
		assertEquals(8, model.countResults(p2));
	}

	@Test
	def void implicitVariableTest() {
		val it = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			
			Person(a).
			Person(b).
			Person(c).
			Person(d).
			friend(a, b).
			friend(a, c).
			friend(a, d).
			friend(b, c).
			
			direct pred has2Friends(x) <->
				exists(x) = true, exists(y) = true, exists(z) = true,
				Person(x) = true, Person(y) = true, Person(z) = true,
				equals(x, y) = false, equals(y, z) = false, equals(x, z) = false,
				friend(x, y) = true, friend(x, z) = true.
			direct pred innerFriend(y) <->
				exists(x) = true, exists(y) = true, exists(z) = true,
				Person(x) = true, Person(y) = true, Person(z) = true,
				equals(x, y) = false, equals(y, z) = false, equals(x, z) = false,
				friend(x, y) = true, friend(y, z) = true.
		''')
		EcoreUtil.resolveAll(it)

		val mapper = new QueryableModelMapper
		val partialModelMapperDTO = mapper.transformProblem(it)
		val model = partialModelMapperDTO.getModel as QueriableModel
		val p1 = model.predicates.findFirst[name == "has2Friends"]
		val p2 = model.predicates.findFirst[name == "innerFriend"]
		assertEquals(1, model.countResults(p1));
		assertEquals(1, model.countResults(p2));
	}

	@Test
	def void negativeDirectPredicateCallTest() {
		val it = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			
			Person(a).
			Person(b).
			Person(c).
			friend(a, b).
			friend(a, c).
			friend(b, a).
			friend(b, c).
			friend(c, a).
			
			direct pred oneDoesNotFriendOfTheOther(x, y) <->
				friend(x, y) = unknown ; friend(y, x) = unknown.
			direct pred p1(x, y) <->
				exists(x) = true, exists(y) = true,
				Person(x) = true, Person(y) = true,
				equals(x, y) = false,
				!oneDoesNotFriendOfTheOther(x, y).
		''')
		EcoreUtil.resolveAll(it)

		val mapper = new QueryableModelMapper
		val partialModelMapperDTO = mapper.transformProblem(it)
		val model = partialModelMapperDTO.getModel as QueriableModel
		val p1 = model.predicates.findFirst[name == "p1"]
		assertEquals(4, model.countResults(p1));
	}
	
}
