package tools.refinery.language.mapping.tests;

import com.google.inject.Inject
import java.util.Map
import java.util.Map.Entry
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.extensions.InjectionExtension
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.^extension.ExtendWith
import tools.refinery.language.mapping.PartialModelMapper
import tools.refinery.language.mapping.PartialModelMapperDTO
import tools.refinery.language.mapping.PartialModelToProblem
import tools.refinery.language.model.problem.Assertion
import tools.refinery.language.model.problem.ClassDeclaration
import tools.refinery.language.model.problem.EnumDeclaration
import tools.refinery.language.model.problem.LogicValue
import tools.refinery.language.model.problem.Node
import tools.refinery.language.model.problem.NodeAssertionArgument
import tools.refinery.language.model.problem.PredicateDefinition
import tools.refinery.language.model.problem.Problem
import tools.refinery.language.model.problem.Statement
import tools.refinery.language.tests.ProblemInjectorProvider
import tools.refinery.store.model.Tuple

import static org.junit.jupiter.api.Assertions.assertTrue
import java.util.Optional

@ExtendWith(InjectionExtension)
@InjectWith(ProblemInjectorProvider)
class PartialModelToProblemTest {
	@Inject
	ParseHelper<Problem> parseHelper
	
	PartialModelMapper mapper
	
	@Inject
	PartialModelToProblem partialModelToProblem
	
	@BeforeEach
	def void beforeEach() {
		mapper = new PartialModelMapper
	}
	
	@Test
	def void relationTest(){
		val problemIn = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			
			friend(a, b).
		''')
		EcoreUtil.resolveAll(problemIn)
		
		val modelAndMaps = mapper.transformProblem(problemIn)
		
		val model = modelAndMaps.model
		val a = findNode("a", modelAndMaps)
		val b = findNode("b", modelAndMaps)
		
		val problemOut = partialModelToProblem.transformModelToProblem(model)
		
		assertTrue(problemOut !== null);
		
		assertTrue(containsStatementWithName(problemOut, "Person"))
		assertTrue(containsStatementWithName(problemOut, "friend"))
		
		assertTrue(containsAssertion(problemOut, "friend", Tuple.of(a, b), LogicValue.TRUE).get)
	}
	
	def Optional<Boolean> containsAssertion(Problem problem, String relationName, Tuple tuple, LogicValue value) {
		for (Statement s: problem.statements){
			if(s instanceof Assertion)	{
				if (s.relation.name.equals(relationName) &&
					s.arguments.size == tuple.size){
					var b = true;
					for(var i = 0; i < tuple.size; i++){
						val argument = s.arguments.get(i)
						if(argument instanceof NodeAssertionArgument){
							if(!argument.node.name.equals(Integer.toString(tuple.get(i)))) b = false
						}
					}
					
					if (b && s.value.equals(value)) { return Optional.of(true);}
					else if(b) { return Optional.of(false); }
				}
			}
		}
		return Optional.empty();
	}
	
	def int findNode(String nodeName, PartialModelMapperDTO dto) {
		for (Entry<Node,Integer> e : dto.nodeMap.entrySet) {
			if(e.key.name.equals(nodeName)) return e.value
		}
		for (Entry<Node,Integer> e : dto.enumNodeMap.entrySet) {
			if(e.key.name.equals(nodeName)) return e.value
		}
		for (Entry<Node,Integer> e : dto.newNodeMap.entrySet) {
			if(e.key.name.equals(nodeName)) return e.value
		}
		for (Entry<Node,Integer> e : dto.uniqueNodeMap.entrySet) {
			if(e.key.name.equals(nodeName)) return e.value
		}
		return -1;
	}
	
	def boolean containsStatementWithName(Problem problem, String statementName) {
		for (Statement s : problem.statements){
			if(s instanceof ClassDeclaration){
				if(s.name.equals(statementName)) return true;
			}
			if(s instanceof EnumDeclaration){
				if(s.name.equals(statementName)) return true;
			}
			if(s instanceof PredicateDefinition){
				if(s.name.equals(statementName)) return true;
			}
		}
		return false
	}
	
	@Test
	def void classTest(){
		val problemIn = parseHelper.parse('''
			class Person {
				Person[0..*] friend
			}
			
			Person(a).
		''')
		EcoreUtil.resolveAll(problemIn)
		
		val modelAndMaps = mapper.transformProblem(problemIn)
		
		val model = modelAndMaps.model
		val a = findNode("a", modelAndMaps)
		
		val problemOut = partialModelToProblem.transformModelToProblem(model)
		
		assertTrue(problemOut !== null);
		
		assertTrue(containsStatementWithName(problemOut,"Person"))
		assertTrue(containsStatementWithName(problemOut, "friend"))
		
		assertTrue(containsAssertion(problemOut, "Person", Tuple.of(a), LogicValue.TRUE).get)
	}
	
	@Test
	def void equalsAndExistTest(){
		val problemIn = parseHelper.parse('''
			node(a).
			node(b).
			
			class Person.
		''')
		EcoreUtil.resolveAll(problemIn)
		
		val modelAndMaps = mapper.transformProblem(problemIn)
		
		val model = modelAndMaps.model
		val a = findNode("a", modelAndMaps)
		val b = findNode("b", modelAndMaps)
		val personNew = findNewNode("Person", modelAndMaps.newNodeMap)
		
		val problemOut = partialModelToProblem.transformModelToProblem(model)
		
		assertTrue(problemOut !== null);
		
		assertTrue(containsStatementWithName(problemOut,"Person"))
		assertTrue(containsStatementWithName(problemOut,"exists"))
		assertTrue(containsStatementWithName(problemOut,"equals"))
		
		assertTrue(containsAssertion(problemOut, "exists", Tuple.of(a), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "exists", Tuple.of(b), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "equals", Tuple.of(a,a), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "equals", Tuple.of(b,b), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "equals", Tuple.of(a,b), LogicValue.FALSE).empty)
		assertTrue(containsAssertion(problemOut, "equals", Tuple.of(b,a), LogicValue.FALSE).empty)
		
		assertTrue(containsAssertion(problemOut, "exists", Tuple.of(personNew), LogicValue.UNKNOWN).get)
		assertTrue(containsAssertion(problemOut, "equals", Tuple.of(personNew,personNew), LogicValue.UNKNOWN).get)
		assertTrue(containsAssertion(problemOut, "equals", Tuple.of(personNew,a), LogicValue.FALSE).empty)
		assertTrue(containsAssertion(problemOut, "equals", Tuple.of(personNew,b), LogicValue.FALSE).empty)
		assertTrue(containsAssertion(problemOut, "equals", Tuple.of(a,personNew), LogicValue.FALSE).empty)
		assertTrue(containsAssertion(problemOut, "equals", Tuple.of(b,personNew), LogicValue.FALSE).empty)
	}
	
	@Test
	def void newNodeTest(){
		val problemIn = parseHelper.parse('''
			class Person.
			abstract class Family.
		''')
		EcoreUtil.resolveAll(problemIn)
		
		val modelAndMaps = mapper.transformProblem(problemIn)
		
		val model = modelAndMaps.model
		val personNew = findNewNode("Person", modelAndMaps.newNodeMap)
		
		val problemOut = partialModelToProblem.transformModelToProblem(model)
		
		assertTrue(problemOut !== null);
		
		assertTrue(containsStatementWithName(problemOut,"Person"))
		assertTrue(containsStatementWithName(problemOut,"Family"))
		
		assertTrue(containsAssertion(problemOut, "Person", Tuple.of(personNew), LogicValue.TRUE).get)
	}
	
	def int findNewNode(String relationName, Map<Node, Integer> newNodeMap) {
		for (Entry<Node,Integer> e : newNodeMap.entrySet){
			val container = e.key.eContainer
			if(container instanceof ClassDeclaration){
				if(container.name.equals(relationName)){
					return e.value
				}
			}
		}
		return -1
	}
	
	@Test
	def void enumTest(){
		val problemIn = parseHelper.parse('''
			enum TaxStatus {
				child, student, adult, retired
			}
		''')
		EcoreUtil.resolveAll(problemIn)
		
		val modelAndMaps = mapper.transformProblem(problemIn)
		
		val model = modelAndMaps.model
		val child = findNode("child", modelAndMaps)
		val student = findNode("student", modelAndMaps)
		val adult = findNode("adult", modelAndMaps)
		val retired = findNode("retired", modelAndMaps)
		
		val problemOut = partialModelToProblem.transformModelToProblem(model)
		
		assertTrue(problemOut !== null);
		
		assertTrue(containsStatementWithName(problemOut, "TaxStatus"))
		
		assertTrue(containsAssertion(problemOut, "TaxStatus", Tuple.of(child), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "TaxStatus", Tuple.of(student), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "TaxStatus", Tuple.of(adult), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "TaxStatus", Tuple.of(retired), LogicValue.TRUE).get)
	}
	
	@Test
	def void builtinBoolTest(){
		val problemIn = parseHelper.parse('''
			class Person.
		''')
		EcoreUtil.resolveAll(problemIn)
		
		val modelAndMaps = mapper.transformProblem(problemIn)
		
		val model = modelAndMaps.model
		val trueNodeValue = findNode("true",modelAndMaps)
		val falseNodeValue = findNode("false",modelAndMaps)
		
		val problemOut = partialModelToProblem.transformModelToProblem(model)
		
		assertTrue(problemOut !== null);
		
		assertTrue(containsStatementWithName(problemOut, "bool"))
		
		assertTrue(containsAssertion(problemOut, "bool", Tuple.of(trueNodeValue), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "bool", Tuple.of(falseNodeValue), LogicValue.TRUE).get)
	}
	
	@Test
	def void compositeTest(){
		val problemIn = parseHelper.parse('''
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
			
			indiv family.
			Family(family).
			members(family, anne): true.
			members(family, bob).
			members(family, ciri).
			children(anne, ciri).
			?children(bob, ciri).
			taxStatus(anne, adult).
		''')
		EcoreUtil.resolveAll(problemIn)
		
		val modelAndMaps = mapper.transformProblem(problemIn)
		
		val model = modelAndMaps.model
		val family = findNode("family", modelAndMaps)
		val anne = findNode("anne", modelAndMaps)
		val bob = findNode("bob", modelAndMaps)
		val ciri = findNode("ciri", modelAndMaps)
		val adult = findNode("adult", modelAndMaps)
		
		
		val problemOut = partialModelToProblem.transformModelToProblem(model)
		
		assertTrue(problemOut !== null);
		
		assertTrue(containsStatementWithName(problemOut, "Family"))
		assertTrue(containsStatementWithName(problemOut, "members"))
		assertTrue(containsStatementWithName(problemOut, "children"))
		assertTrue(containsStatementWithName(problemOut, "parent"))
		assertTrue(containsStatementWithName(problemOut, "taxStatus"))
		assertTrue(containsStatementWithName(problemOut, "TaxStatus"))
		assertTrue(containsStatementWithName(problemOut, "invalidTaxStatus"))
		
		assertTrue(containsAssertion(problemOut, "Family", Tuple.of(family), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "members", Tuple.of(family, anne), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "members", Tuple.of(family, bob), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "members", Tuple.of(family, ciri), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "children", Tuple.of(anne, ciri), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "children", Tuple.of(bob, ciri), LogicValue.UNKNOWN).get)
		assertTrue(containsAssertion(problemOut, "taxStatus", Tuple.of(anne, adult), LogicValue.TRUE).get)
	}
	
	@Test
	def void carCaseStudyTest(){
		val problemIn = parseHelper.parse('''
			abstract class DynamicComponent {
				contains StaticComponent[1..1] placedOn
			}
			abstract class StaticComponent.
			class Car extends DynamicComponent.
			class Pedestrian extends DynamicComponent.
			class Road extends StaticComponent {
				contains LaneSegment[0..*] lanes
			}
			class LaneSegment extends StaticComponent {
				Lane[0..*] adjacentLanes
				Lane[0..*] sameDirLanes
			}
			
			Car(c1).
			Car(c2).
			Pedestrian(p1).
			Road(r1).
			LaneSegment(l1).
			LaneSegment(l2).
			LaneSegment(l3).
			placedOn(c1,l1).
			placedOn(c2,l2).
			placedOn(p1,l3).
			lanes(r1,l1).
			lanes(r1,l2).
			lanes(r1,l3).
			adjacentLanes(l1,l2).
			adjacentLanes(l2,l1).
			sameDirLanes(l1,l3).
			sameDirLanes(l3,l1).
		''')
		EcoreUtil.resolveAll(problemIn)
		
		val modelAndMaps = mapper.transformProblem(problemIn)
		
		val model = modelAndMaps.model
		val c1 = findNode("c1",modelAndMaps)
		val c2 = findNode("c2",modelAndMaps)
		val p1 = findNode("p1",modelAndMaps)
		val r1 = findNode("r1",modelAndMaps)
		val l1 = findNode("l1",modelAndMaps)
		val l2 = findNode("l2",modelAndMaps)
		val l3 = findNode("l3",modelAndMaps)
		
		
		val problemOut = partialModelToProblem.transformModelToProblem(model)
		
		assertTrue(problemOut !== null);
		
		assertTrue(containsStatementWithName(problemOut, "DynamicComponent"))
		assertTrue(containsStatementWithName(problemOut, "placedOn"))
		assertTrue(containsStatementWithName(problemOut, "StaticComponent"))
		assertTrue(containsStatementWithName(problemOut, "Car"))
		assertTrue(containsStatementWithName(problemOut, "Pedestrian"))
		assertTrue(containsStatementWithName(problemOut, "Road"))
		assertTrue(containsStatementWithName(problemOut, "lanes"))
		assertTrue(containsStatementWithName(problemOut, "LaneSegment"))
		assertTrue(containsStatementWithName(problemOut, "adjacentLanes"))
		assertTrue(containsStatementWithName(problemOut, "sameDirLanes"))
		
		assertTrue(containsAssertion(problemOut, "Car", Tuple.of(c1), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "Car", Tuple.of(c2), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "Pedestrian", Tuple.of(p1), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "Road", Tuple.of(r1), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "LaneSegment", Tuple.of(l1), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "LaneSegment", Tuple.of(l2), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "LaneSegment", Tuple.of(l3), LogicValue.TRUE).get)
		
		assertTrue(containsAssertion(problemOut, "placedOn", Tuple.of(c1, l1), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "placedOn", Tuple.of(c2, l2), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "placedOn", Tuple.of(p1, l3), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "lanes", Tuple.of(r1, l1), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "lanes", Tuple.of(r1, l2), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "lanes", Tuple.of(r1, l3), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "adjacentLanes", Tuple.of(l1, l2), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "adjacentLanes", Tuple.of(l2, l1), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "sameDirLanes", Tuple.of(l1, l3), LogicValue.TRUE).get)
		assertTrue(containsAssertion(problemOut, "sameDirLanes", Tuple.of(l3, l1), LogicValue.TRUE).get)
	}
}
