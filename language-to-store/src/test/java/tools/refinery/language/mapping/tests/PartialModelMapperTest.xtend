package tools.refinery.language.mapping.tests

import com.google.inject.Inject
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import tools.refinery.language.mapping.PartialModelMapper
import tools.refinery.language.model.problem.Problem
import tools.refinery.language.model.tests.ProblemTestUtil
import tools.refinery.language.tests.ProblemInjectorProvider
import tools.refinery.store.model.Tuple
import tools.refinery.store.model.representation.TruthValue

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*
import static org.junit.jupiter.api.Assertions.assertTrue

@ExtendWith(InjectionExtension)
@InjectWith(ProblemInjectorProvider)
class PartialModelMapperTest {
	@Inject
	ParseHelper<Problem> parseHelper
	
	@Inject
	extension ProblemTestUtil

	PartialModelMapper mapper

	@BeforeEach
	def void beforeEach() {
		mapper = new PartialModelMapper
	}

	//Testing the relation
	@Test
	def void relationTest() {
		val problem = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			
			friend(a, b).
		''')
		EcoreUtil.resolveAll(problem)
		
		val modelAndMaps = mapper.transformProblem(problem)
		assertThat(modelAndMaps, notNullValue())
		
		val model = modelAndMaps.model
		val relationMap = modelAndMaps.relationMap
		val nodeMap = modelAndMaps.nodeMap
		
		val person = problem.findClass("Person")
		val friend = problem.findClass("Person").reference("friend")
		val a = problem.node("a")
		val b = problem.node("b")
		
		assertTrue(model.getDataRepresentations().contains(relationMap.get(person)));
		assertTrue(model.getDataRepresentations().contains(relationMap.get(friend)));
		assertTrue(model.get(relationMap.get(friend), Tuple.of(nodeMap.get(a),nodeMap.get(b))).equals(TruthValue.TRUE));
	}
	
	//Testing the class
	@Test
	def void classTest() {
		val problem = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			
			Person(a).
		''')
		EcoreUtil.resolveAll(problem)
		
		val modelAndMaps = mapper.transformProblem(problem)
		assertThat(modelAndMaps, notNullValue())
		
		val model = modelAndMaps.model
		val relationMap = modelAndMaps.relationMap
		val nodeMap = modelAndMaps.nodeMap
		
		val person = problem.findClass("Person")
		val friend = problem.findClass("Person").reference("friend")
		val a = problem.node("a")
		
		assertTrue(model.getDataRepresentations().contains(relationMap.get(person)));
		assertTrue(model.getDataRepresentations().contains(relationMap.get(friend)));
		
		assertTrue(model.get(relationMap.get(person), Tuple.of(nodeMap.get(a))).equals(TruthValue.TRUE));
	}
	
	//Testing the equals and exists from the built in problem
	@Test
	def void equalsAndExistTest() {
		val problem = parseHelper.parse('''
			node(a).
			node(b).
			
			class Person.
		''')
		EcoreUtil.resolveAll(problem)
		val builtin = problem.builtin;
		
		val modelAndMaps = mapper.transformProblem(problem)
		assertThat(modelAndMaps, notNullValue())
		
		val model = modelAndMaps.model
		val relationMap = modelAndMaps.relationMap
		val nodeMap = modelAndMaps.nodeMap
		val newNodeMap = modelAndMaps.newNodeMap
		
		val a = problem.node("a")
		val b = problem.node("b")
		val Person = problem.findClass("Person")
		val PersonNew = problem.findClass("Person").newNode
		val exists = builtin.pred("exists")
		val equals = builtin.findClass("node").reference("equals")
		
		assertTrue(model.getDataRepresentations().contains(relationMap.get(Person)))
		assertTrue(model.getDataRepresentations().contains(relationMap.get(exists)))
		assertTrue(model.getDataRepresentations().contains(relationMap.get(equals)))
		
		assertTrue(model.get(relationMap.get(exists), Tuple.of(nodeMap.get(a))).equals(TruthValue.TRUE))
		assertTrue(model.get(relationMap.get(exists), Tuple.of(nodeMap.get(b))).equals(TruthValue.TRUE))
		assertTrue(model.get(relationMap.get(equals), Tuple.of(nodeMap.get(a), nodeMap.get(a))).equals(TruthValue.TRUE))
		assertTrue(model.get(relationMap.get(equals), Tuple.of(nodeMap.get(b), nodeMap.get(b))).equals(TruthValue.TRUE))
		assertTrue(model.get(relationMap.get(equals), Tuple.of(nodeMap.get(a), nodeMap.get(b))).equals(TruthValue.FALSE))
		assertTrue(model.get(relationMap.get(equals), Tuple.of(nodeMap.get(b), nodeMap.get(a))).equals(TruthValue.FALSE))
		
		assertTrue(model.get(relationMap.get(exists), Tuple.of(newNodeMap.get(PersonNew))).equals(TruthValue.UNKNOWN))
		assertTrue(model.get(relationMap.get(equals), Tuple.of(newNodeMap.get(PersonNew), newNodeMap.get(PersonNew))).equals(TruthValue.UNKNOWN))
		assertTrue(model.get(relationMap.get(equals), Tuple.of(newNodeMap.get(PersonNew), nodeMap.get(a))).equals(TruthValue.FALSE))
		assertTrue(model.get(relationMap.get(equals), Tuple.of(newNodeMap.get(PersonNew), nodeMap.get(b))).equals(TruthValue.FALSE))
		assertTrue(model.get(relationMap.get(equals), Tuple.of(nodeMap.get(a), newNodeMap.get(PersonNew))).equals(TruthValue.FALSE))
		assertTrue(model.get(relationMap.get(equals), Tuple.of(nodeMap.get(b), newNodeMap.get(PersonNew))).equals(TruthValue.FALSE))
	}
	
	//Testing the equals and exists from the built in problem with a different example
	@Test
	def void equalsAndExistTest2() {
		val problem = parseHelper.parse('''
			class Person.
			
			Person(a).
			Person(b).
		''')
		val builtin = problem.builtin;
		EcoreUtil.resolveAll(problem)
		
		val modelAndMaps = mapper.transformProblem(problem)
		assertThat(modelAndMaps, notNullValue())
		
		val model = modelAndMaps.model
		val relationMap = modelAndMaps.relationMap
		val nodeMap = modelAndMaps.nodeMap
		val newNodeMap = modelAndMaps.newNodeMap
		
		val a = problem.node("a")
		val b = problem.node("b")
		val Person = problem.findClass("Person")
		val PersonNew = problem.findClass("Person").newNode
		val exists = builtin.pred("exists")
		val equals = builtin.findClass("node").reference("equals")
		
		assertTrue(model.getDataRepresentations().contains(relationMap.get(Person)));
		assertTrue(model.getDataRepresentations().contains(relationMap.get(exists)));
		assertTrue(model.getDataRepresentations().contains(relationMap.get(equals)));
		
		assertTrue(model.get(relationMap.get(exists), Tuple.of(nodeMap.get(a))).equals(TruthValue.TRUE));
		assertTrue(model.get(relationMap.get(exists), Tuple.of(nodeMap.get(b))).equals(TruthValue.TRUE));
		assertTrue(model.get(relationMap.get(equals), Tuple.of(nodeMap.get(a), nodeMap.get(a))).equals(TruthValue.TRUE));
		assertTrue(model.get(relationMap.get(equals), Tuple.of(nodeMap.get(b), nodeMap.get(b))).equals(TruthValue.TRUE));
		assertTrue(model.get(relationMap.get(equals), Tuple.of(nodeMap.get(a), nodeMap.get(b))).equals(TruthValue.FALSE));
		assertTrue(model.get(relationMap.get(equals), Tuple.of(nodeMap.get(b), nodeMap.get(a))).equals(TruthValue.FALSE));
		
		assertTrue(model.get(relationMap.get(exists), Tuple.of(newNodeMap.get(PersonNew))).equals(TruthValue.UNKNOWN));
		assertTrue(model.get(relationMap.get(equals), Tuple.of(newNodeMap.get(PersonNew), newNodeMap.get(PersonNew))).equals(TruthValue.UNKNOWN));
		assertTrue(model.get(relationMap.get(equals), Tuple.of(newNodeMap.get(PersonNew), nodeMap.get(a))).equals(TruthValue.FALSE));
		assertTrue(model.get(relationMap.get(equals), Tuple.of(newNodeMap.get(PersonNew), nodeMap.get(b))).equals(TruthValue.FALSE));
		assertTrue(model.get(relationMap.get(equals), Tuple.of(nodeMap.get(a), newNodeMap.get(PersonNew))).equals(TruthValue.FALSE));
		assertTrue(model.get(relationMap.get(equals), Tuple.of(nodeMap.get(b), newNodeMap.get(PersonNew))).equals(TruthValue.FALSE));	
	}
	
	//Testing the behavior of the newNodes
	@Test
	def void newNodeTest(){
		val problem = parseHelper.parse('''
			class Person.
			abstract class Family.
		''')
		EcoreUtil.resolveAll(problem)
		
		val modelAndMaps = mapper.transformProblem(problem);
		assertThat(modelAndMaps, notNullValue())
		
		val model = modelAndMaps.model
		val relationMap = modelAndMaps.relationMap
		val newNodeMap = modelAndMaps.newNodeMap
		
		val Person = problem.findClass("Person")
		val Family = problem.findClass("Family")
		val PersonNew = problem.findClass("Person").newNode
		
		
		assertTrue(model.getDataRepresentations().contains(relationMap.get(Person)));
		assertTrue(model.getDataRepresentations().contains(relationMap.get(Family)));
		
		assertTrue(newNodeMap.size.equals(4)) //3 from builtin.problem, 1 from Person
		assertTrue(model.get(relationMap.get(Person), Tuple.of(newNodeMap.get(PersonNew))).equals(TruthValue.TRUE));
	}

	//Testing the behavior of enumerations
	@Test
	def void enumTest(){
		val problem = parseHelper.parse('''
			enum TaxStatus {
				child, student, adult, retired
			}
		''')
		EcoreUtil.resolveAll(problem)
		
		val modelAndMaps = mapper.transformProblem(problem)
		assertThat(modelAndMaps, notNullValue())
		
		val model = modelAndMaps.model
		val relationMap = modelAndMaps.relationMap
		val enumNodeMap = modelAndMaps.enumNodeMap
		
		val TaxStatus = problem.findEnum("TaxStatus")
		val child = problem.findEnum("TaxStatus").literal("child")
		val student = problem.findEnum("TaxStatus").literal("student")
		val adult = problem.findEnum("TaxStatus").literal("adult")
		val retired = problem.findEnum("TaxStatus").literal("retired")
		
		assertTrue(model.getDataRepresentations().contains(relationMap.get(TaxStatus)));
		assertTrue(model.get(relationMap.get(TaxStatus), Tuple.of(enumNodeMap.get(child))).equals(TruthValue.TRUE));
		assertTrue(model.get(relationMap.get(TaxStatus), Tuple.of(enumNodeMap.get(student))).equals(TruthValue.TRUE));
		assertTrue(model.get(relationMap.get(TaxStatus), Tuple.of(enumNodeMap.get(adult))).equals(TruthValue.TRUE));
		assertTrue(model.get(relationMap.get(TaxStatus), Tuple.of(enumNodeMap.get(retired))).equals(TruthValue.TRUE));
	}
	
	//Testing the bool from the built in problem
	@Test
	def void builtinBoolTest(){
		val problem = parseHelper.parse('''
			class Person.
		''')
		EcoreUtil.resolveAll(problem)
		val builtin = problem.builtin;
		
		val modelAndMaps = mapper.transformProblem(problem)
		assertThat(modelAndMaps, notNullValue())
		
		val model = modelAndMaps.model
		val relationMap = modelAndMaps.relationMap
		val enumNodeMap = modelAndMaps.enumNodeMap
		
		val bool = builtin.findEnum("bool")
		val trueEnum = builtin.findEnum("bool").literal("true") //Emiatt nem sikerül a teszt
		val falseEnum = builtin.findEnum("bool").literal("false")
		
		assertTrue(model.getDataRepresentations().contains(relationMap.get(bool)));
		assertTrue(model.get(relationMap.get(bool), Tuple.of(enumNodeMap.get(trueEnum))).equals(TruthValue.TRUE));
		assertTrue(model.get(relationMap.get(bool), Tuple.of(enumNodeMap.get(falseEnum))).equals(TruthValue.TRUE));
	}
	
	//Testing different aspects of the behavior
	@Test
	def void compositeTest() {
		val problem = parseHelper.parse('''
			class Family {
				contains Person[] members
			}
			
			class Person {
				Person[0..*] children
				Person[0..1] parent
				TaxStatus taxStatus
			}
			
			enum TaxStatus {
				child, student, adult, retired
			}
			
			% A child cannot have any dependents.
			error invalidTaxStatus(Person p) <->
				taxStatus(p, child), children(p, _q).
			
			unique family.
			Family(family).
			members(family, anne): true.
			members(family, bob).
			members(family, ciri).
			children(anne, ciri).
			?children(bob, ciri).
			taxStatus(anne, adult).
		''')
		EcoreUtil.resolveAll(problem)
		
		val modelAndMaps = mapper.transformProblem(problem)
		assertThat(modelAndMaps, notNullValue())
		
		val model = modelAndMaps.model
		val relationMap = modelAndMaps.relationMap
		val nodeMap = modelAndMaps.nodeMap
		val uniqueNodeMap = modelAndMaps.uniqueNodeMap
		val enumNodeMap = modelAndMaps.enumNodeMap
		
		val Family = problem.findClass("Family")
		val members = problem.findClass("Family").reference("members")
		val Person = problem.findClass("Person")
		val children = problem.findClass("Person").reference("children")
		val parent = problem.findClass("Person").reference("parent")
		val taxStatus = problem.findClass("Person").reference("taxStatus")
		val TaxStatus = problem.findEnum("TaxStatus")
		val invalidTaxStatus = problem.pred("invalidTaxStatus")
		
		val anne = problem.node("anne")
		val bob = problem.node("bob")
		val ciri = problem.node("ciri")
		val family = problem.uniqueNode("family")
		val adult = problem.findEnum("TaxStatus").literal("adult")
		
		assertTrue(model.getDataRepresentations().contains(relationMap.get(Family)));
		assertTrue(model.getDataRepresentations().contains(relationMap.get(members)));
		assertTrue(model.getDataRepresentations().contains(relationMap.get(Person)));
		assertTrue(model.getDataRepresentations().contains(relationMap.get(children)));
		assertTrue(model.getDataRepresentations().contains(relationMap.get(parent)));
		assertTrue(model.getDataRepresentations().contains(relationMap.get(taxStatus)));
		assertTrue(model.getDataRepresentations().contains(relationMap.get(TaxStatus)));
		assertTrue(model.getDataRepresentations().contains(relationMap.get(invalidTaxStatus)));
		
		assertTrue(model.get(relationMap.get(Family), Tuple.of(uniqueNodeMap.get(family))).equals(TruthValue.TRUE));
		assertTrue(model.get(relationMap.get(members), Tuple.of(uniqueNodeMap.get(family),nodeMap.get(anne))).equals(TruthValue.TRUE));
		assertTrue(model.get(relationMap.get(members), Tuple.of(uniqueNodeMap.get(family),nodeMap.get(bob))).equals(TruthValue.TRUE));
		assertTrue(model.get(relationMap.get(members), Tuple.of(uniqueNodeMap.get(family),nodeMap.get(ciri))).equals(TruthValue.TRUE));
		assertTrue(model.get(relationMap.get(children), Tuple.of(nodeMap.get(anne),nodeMap.get(ciri))).equals(TruthValue.TRUE));
		assertTrue(model.get(relationMap.get(children), Tuple.of(nodeMap.get(bob),nodeMap.get(ciri))).equals(TruthValue.UNKNOWN));
		assertTrue(model.get(relationMap.get(taxStatus), Tuple.of(nodeMap.get(anne),enumNodeMap.get(adult))).equals(TruthValue.TRUE));
	}
}
