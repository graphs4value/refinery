package tools.refinery.language.mapping.tests

import tools.refinery.language.ProblemUtil
import tools.refinery.language.model.problem.Argument
import tools.refinery.language.model.problem.Assertion
import tools.refinery.language.model.problem.AssertionArgument
import tools.refinery.language.model.problem.Atom
import tools.refinery.language.model.problem.ClassDeclaration
import tools.refinery.language.model.problem.Conjunction
import tools.refinery.language.model.problem.EnumDeclaration
import tools.refinery.language.model.problem.Literal
import tools.refinery.language.model.problem.NegativeLiteral
import tools.refinery.language.model.problem.Node
import tools.refinery.language.model.problem.NodeAssertionArgument
import tools.refinery.language.model.problem.NodeValueAssertion
import tools.refinery.language.model.problem.PredicateDefinition
import tools.refinery.language.model.problem.Problem
import tools.refinery.language.model.problem.UniqueDeclaration
import tools.refinery.language.model.problem.Variable
import tools.refinery.language.model.problem.VariableOrNodeArgument

class ProblemTestUtil {
	def builtin(Problem it) {
		ProblemUtil.getBuiltInLibrary(it).get
	}
	
	def errors(Problem it) {
		eResource.errors
	}
	
	def nodeNames(Problem it) {
		nodes.map[name]
	}
	
	def pred(Problem it, String name) {
		statements.filter(PredicateDefinition).findFirst[it.name == name]
	}
	
	def param(PredicateDefinition it, int i) {
		parameters.get(i)
	} 

	def conj(PredicateDefinition it, int i) {
		bodies.get(i)
	}

	def lit(Conjunction it, int i) {
		literals.get(i)
	}

	def negated(Literal it) {
		(it as NegativeLiteral).atom
	}

	def relation(Literal it) {
		(it as Atom).relation
	}

	def arg(Atom it, int i) {
		it.arguments.get(i)
	}

	def arg(Literal it, int i) {
		(it as Atom).arg(i)
	}

	def variable(Argument it) {
		(it as VariableOrNodeArgument).variableOrNode as Variable
	}

	def node(Argument it) {
		(it as VariableOrNodeArgument).variableOrNode as Node
	}
	
	def assertion(Problem it, int i) {
		statements.filter(Assertion).get(i)
	}
	
	def nodeValueAssertion(Problem it, int i) {
		statements.filter(NodeValueAssertion).get(i)
	}
	
	def arg(Assertion it, int i) {
		arguments.get(i)
	}
	
	def node(AssertionArgument it) {
		(it as NodeAssertionArgument).node
	}
	
	def node(Problem it, String name) {
		nodes.findFirst[it.name == name]
	}
	
	def uniqueNode(Problem it, String name) {
		statements.filter(UniqueDeclaration).flatMap[nodes].findFirst[it.name == name]
	}
	
	def findClass(Problem it, String name) {
		statements.filter(ClassDeclaration).findFirst[it.name == name]
	}
	
	def reference(ClassDeclaration it, String name) {
		it.referenceDeclarations.findFirst[it.name == name]
	}
	
	def findEnum(Problem it, String name) {
		statements.filter(EnumDeclaration).findFirst[it.name == name]
	}
	
	def literal(EnumDeclaration it, String name) {
		literals.findFirst[it.name == name]
	}
}
