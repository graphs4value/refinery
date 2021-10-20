package tools.refinery.language.tests.serializer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.inject.Inject;

import tools.refinery.language.model.ProblemUtil;
import tools.refinery.language.model.problem.Atom;
import tools.refinery.language.model.problem.LogicValue;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.PredicateDefinition;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemFactory;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.language.model.problem.VariableOrNode;
import tools.refinery.language.model.tests.ProblemTestUtil;
import tools.refinery.language.tests.ProblemInjectorProvider;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class ProblemSerializerTest {
	@Inject
	private ResourceSet resourceSet;

	@Inject
	private ProblemTestUtil testUtil;

	private Resource resource;

	private Problem problem;

	private Problem builtin;

	@BeforeEach
	void beforeEach() {
		problem = ProblemFactory.eINSTANCE.createProblem();
		resource = resourceSet.createResource(URI.createFileURI("test.problem"));
		resource.getContents().add(problem);
		builtin = ProblemUtil.getBuiltInLibrary(problem).get();
	}

	@ParameterizedTest
	@MethodSource
	void assertionTest(LogicValue value, String serializedAssertion) {
		var pred = createPred();
		var node = ProblemFactory.eINSTANCE.createNode();
		node.setName("a");
		var individualDeclaration = ProblemFactory.eINSTANCE.createIndividualDeclaration();
		individualDeclaration.getNodes().add(node);
		problem.getStatements().add(individualDeclaration);
		createAssertion(pred, node, value);

		assertSerializedResult("""
				pred foo(node p).

				indiv a.
				""" + serializedAssertion + "\n");
	}

	static Stream<Arguments> assertionTest() {
		return Stream.of(Arguments.of(LogicValue.TRUE, "foo(a)."), Arguments.of(LogicValue.FALSE, "!foo(a)."),
				Arguments.of(LogicValue.UNKNOWN, "?foo(a)."), Arguments.of(LogicValue.ERROR, "foo(a): error."));
	}

	@Test
	void implicitNodeTest() {
		var pred = createPred();
		var node = ProblemFactory.eINSTANCE.createNode();
		node.setName("a");
		problem.getNodes().add(node);
		createAssertion(pred, node);

		assertSerializedResult("""
				pred foo(node p).

				foo(a).
				""");
	}

	private PredicateDefinition createPred() {
		var pred = ProblemFactory.eINSTANCE.createPredicateDefinition();
		pred.setName("foo");
		var parameter = ProblemFactory.eINSTANCE.createParameter();
		var nodeType = testUtil.findClass(builtin, "node");
		parameter.setParameterType(nodeType);
		parameter.setName("p");
		pred.getParameters().add(parameter);
		problem.getStatements().add(pred);
		return pred;
	}

	@Test
	void newNodeTest() {
		var classDeclaration = ProblemFactory.eINSTANCE.createClassDeclaration();
		classDeclaration.setName("Foo");
		var newNode = ProblemFactory.eINSTANCE.createNode();
		newNode.setName("new");
		classDeclaration.setNewNode(newNode);
		problem.getStatements().add(classDeclaration);
		createAssertion(classDeclaration, newNode);

		assertSerializedResult("""
				class Foo.

				Foo(Foo::new).
				""");
	}

	private void createAssertion(Relation relation, Node node) {
		createAssertion(relation, node, LogicValue.TRUE);
	}

	private void createAssertion(Relation relation, Node node, LogicValue value) {
		var assertion = ProblemFactory.eINSTANCE.createAssertion();
		assertion.setRelation(relation);
		var argument = ProblemFactory.eINSTANCE.createNodeAssertionArgument();
		argument.setNode(node);
		assertion.getArguments().add(argument);
		assertion.setValue(value);
		problem.getStatements().add(assertion);
	}

	@Test
	void implicitVariableTest() {
		var pred = ProblemFactory.eINSTANCE.createPredicateDefinition();
		pred.setName("foo");
		var nodeType = testUtil.findClass(builtin, "node");
		var parameter1 = ProblemFactory.eINSTANCE.createParameter();
		parameter1.setParameterType(nodeType);
		parameter1.setName("p1");
		pred.getParameters().add(parameter1);
		var parameter2 = ProblemFactory.eINSTANCE.createParameter();
		parameter2.setParameterType(nodeType);
		parameter2.setName("p2");
		pred.getParameters().add(parameter2);
		var conjunction = ProblemFactory.eINSTANCE.createConjunction();
		var variable = ProblemFactory.eINSTANCE.createImplicitVariable();
		variable.setName("q");
		conjunction.getImplicitVariables().add(variable);
		var equals = testUtil.reference(nodeType, "equals");
		conjunction.getLiterals().add(createAtom(equals, parameter1, variable));
		conjunction.getLiterals().add(createAtom(equals, variable, parameter2));
		pred.getBodies().add(conjunction);
		problem.getStatements().add(pred);

		assertSerializedResult("""
				pred foo(node p1, node p2) <-> equals(p1, q), equals(q, p2).
				""");
	}

	private Atom createAtom(Relation relation, VariableOrNode variable1, VariableOrNode variable2) {
		var atom = ProblemFactory.eINSTANCE.createAtom();
		atom.setRelation(relation);
		var arg1 = ProblemFactory.eINSTANCE.createVariableOrNodeArgument();
		arg1.setVariableOrNode(variable1);
		atom.getArguments().add(arg1);
		var arg2 = ProblemFactory.eINSTANCE.createVariableOrNodeArgument();
		arg2.setVariableOrNode(variable2);
		atom.getArguments().add(arg2);
		return atom;
	}

	@Test
	void singletonVariableTest() {
		var pred = ProblemFactory.eINSTANCE.createPredicateDefinition();
		pred.setName("foo");
		var nodeType = testUtil.findClass(builtin, "node");
		var parameter = ProblemFactory.eINSTANCE.createParameter();
		parameter.setParameterType(nodeType);
		parameter.setName("p");
		pred.getParameters().add(parameter);
		var conjunction = ProblemFactory.eINSTANCE.createConjunction();
		var atom = ProblemFactory.eINSTANCE.createAtom();
		var equals = testUtil.reference(nodeType, "equals");
		atom.setRelation(equals);
		var arg1 = ProblemFactory.eINSTANCE.createVariableOrNodeArgument();
		arg1.setVariableOrNode(parameter);
		atom.getArguments().add(arg1);
		var arg2 = ProblemFactory.eINSTANCE.createVariableOrNodeArgument();
		var variable = ProblemFactory.eINSTANCE.createImplicitVariable();
		variable.setName("_q");
		arg2.setSingletonVariable(variable);
		arg2.setVariableOrNode(variable);
		atom.getArguments().add(arg2);
		conjunction.getLiterals().add(atom);
		pred.getBodies().add(conjunction);
		problem.getStatements().add(pred);

		assertSerializedResult("""
				pred foo(node p) <-> equals(p, _q).
				""");
	}

	private void assertSerializedResult(String expected) {
		var outputStream = new ByteArrayOutputStream();
		try {
			resource.save(outputStream, Map.of());
		} catch (IOException e) {
			throw new AssertionError("Failed to serialize problem", e);
		} finally {
			try {
				outputStream.close();
			} catch (IOException e) {
				// Nothing to handle in a test.
			}
		}
		var problemString = outputStream.toString();
		var systemNewline = System.getProperty("line.separator");
		if (systemNewline != null) {
			problemString = problemString.replace(systemNewline, "\n");
		}
		assertThat(problemString, equalTo(expected));
	}
}
