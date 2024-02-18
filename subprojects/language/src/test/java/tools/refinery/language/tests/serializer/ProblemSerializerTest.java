/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.serializer;

import com.google.inject.Inject;
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
import tools.refinery.language.model.problem.*;
import tools.refinery.language.model.tests.utils.WrappedProblem;
import tools.refinery.language.tests.ProblemInjectorProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class ProblemSerializerTest {
	@Inject
	private ResourceSet resourceSet;

	private Resource resource;

	private Problem problem;

	private WrappedProblem builtin;

	@BeforeEach
	void beforeEach() {
		problem = ProblemFactory.eINSTANCE.createProblem();
		resource = resourceSet.createResource(URI.createFileURI("test.problem"));
		resource.getContents().add(problem);
		var wrappedProblem = new WrappedProblem(problem);
		builtin = wrappedProblem.builtin();
	}

	@ParameterizedTest
	@MethodSource
	void assertionTest(LogicValue value, String serializedAssertion) {
		var pred = createPred();
		var node = ProblemFactory.eINSTANCE.createNode();
		node.setName("a");
		var atomDeclaration = ProblemFactory.eINSTANCE.createNodeDeclaration();
		atomDeclaration.setKind(NodeKind.ATOM);
		atomDeclaration.getNodes().add(node);
		problem.getStatements().add(atomDeclaration);
		createAssertion(pred, node, value);

		assertSerializedResult("""
				pred foo(node p).

				atom a.
				""" + serializedAssertion + "\n");
	}

	static Stream<Arguments> assertionTest() {
		return Stream.of(Arguments.of(LogicValue.TRUE, "foo(a)."), Arguments.of(LogicValue.FALSE, "!foo(a)."),
				Arguments.of(LogicValue.UNKNOWN, "?foo(a)."), Arguments.of(LogicValue.ERROR, "foo(a): error."));
	}

	@ParameterizedTest
	@MethodSource("assertionTest")
	void defaultAssertionTest(LogicValue value, String serializedAssertion) {
		var pred = createPred();
		var node = ProblemFactory.eINSTANCE.createNode();
		node.setName("a");
		var atomDeclaration = ProblemFactory.eINSTANCE.createNodeDeclaration();
		atomDeclaration.setKind(NodeKind.ATOM);
		atomDeclaration.getNodes().add(node);
		problem.getStatements().add(atomDeclaration);
		createAssertion(pred, node, value, true);

		assertSerializedResult("""
				pred foo(node p).

				atom a.
				default\040""" + serializedAssertion + "\n");
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
		var nodeType = builtin.findClass("node");
		parameter.setParameterType(nodeType.get());
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

	private void createAssertion(Relation relation, Node node, LogicValue logicValue) {
		createAssertion(relation, node, logicValue, false);
	}

	private void createAssertion(Relation relation, Node node, LogicValue logicValue, boolean isDefault) {
		var assertion = ProblemFactory.eINSTANCE.createAssertion();
		assertion.setRelation(relation);
		var argument = ProblemFactory.eINSTANCE.createNodeAssertionArgument();
		argument.setNode(node);
		assertion.getArguments().add(argument);
		var value = ProblemFactory.eINSTANCE.createLogicConstant();
		value.setLogicValue(logicValue);
		assertion.setValue(value);
		assertion.setDefault(isDefault);
		problem.getStatements().add(assertion);
	}


	@Test
	void implicitVariableTest() {
		var pred = ProblemFactory.eINSTANCE.createPredicateDefinition();
		pred.setName("foo");
		var nodeType = builtin.findClass("node");
		var parameter1 = ProblemFactory.eINSTANCE.createParameter();
		parameter1.setParameterType(nodeType.get());
		parameter1.setName("p1");
		pred.getParameters().add(parameter1);
		var parameter2 = ProblemFactory.eINSTANCE.createParameter();
		parameter2.setParameterType(nodeType.get());
		parameter2.setName("p2");
		pred.getParameters().add(parameter2);
		var conjunction = ProblemFactory.eINSTANCE.createConjunction();
		var variable = ProblemFactory.eINSTANCE.createImplicitVariable();
		variable.setName("q");
		conjunction.getImplicitVariables().add(variable);
		var equals = builtin.pred("equals").get();
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
		var arg1 = ProblemFactory.eINSTANCE.createVariableOrNodeExpr();
		arg1.setVariableOrNode(variable1);
		atom.getArguments().add(arg1);
		var arg2 = ProblemFactory.eINSTANCE.createVariableOrNodeExpr();
		arg2.setVariableOrNode(variable2);
		atom.getArguments().add(arg2);
		return atom;
	}

	@Test
	void singletonVariableTest() {
		var pred = ProblemFactory.eINSTANCE.createPredicateDefinition();
		pred.setName("foo");
		var nodeType = builtin.findClass("node");
		var parameter = ProblemFactory.eINSTANCE.createParameter();
		parameter.setParameterType(nodeType.get());
		parameter.setName("p");
		pred.getParameters().add(parameter);
		var conjunction = ProblemFactory.eINSTANCE.createConjunction();
		var atom = ProblemFactory.eINSTANCE.createAtom();
		var equals = builtin.pred("equals").get();
		atom.setRelation(equals);
		var arg1 = ProblemFactory.eINSTANCE.createVariableOrNodeExpr();
		arg1.setVariableOrNode(parameter);
		atom.getArguments().add(arg1);
		var arg2 = ProblemFactory.eINSTANCE.createVariableOrNodeExpr();
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

	@Test
	void unambiguousNameTest() {
		createClassAndAssertion("Foo", "foo");

		assertSerializedResult("""
				class Foo {
					Foo ref
				}

				ref(foo, foo).
				""");
	}

	@Test
	void ambiguousNameTest() {
		createClassAndAssertion("Foo", "foo");
		createClassAndAssertion("Bar", "bar");

		assertSerializedResult("""
				class Foo {
					Foo ref
				}

				Foo::ref(foo, foo).

				class Bar {
					Bar ref
				}

				Bar::ref(bar, bar).
				""");
	}

	private void createClassAndAssertion(String className, String nodeName) {
		var classDeclaration = ProblemFactory.eINSTANCE.createClassDeclaration();
		classDeclaration.setName(className);
		var referenceDeclaration = ProblemFactory.eINSTANCE.createReferenceDeclaration();
		referenceDeclaration.setReferenceType(classDeclaration);
		referenceDeclaration.setName("ref");
		classDeclaration.getFeatureDeclarations().add(referenceDeclaration);
		problem.getStatements().add(classDeclaration);
		var node = ProblemFactory.eINSTANCE.createNode();
		node.setName(nodeName);
		problem.getNodes().add(node);
		createBinaryAssertion(referenceDeclaration, node, node);
	}

	private void createBinaryAssertion(Relation relation, Node from, Node to) {
		var assertion = ProblemFactory.eINSTANCE.createAssertion();
		assertion.setRelation(relation);
		var fromArgument = ProblemFactory.eINSTANCE.createNodeAssertionArgument();
		fromArgument.setNode(from);
		assertion.getArguments().add(fromArgument);
		var toArgument = ProblemFactory.eINSTANCE.createNodeAssertionArgument();
		toArgument.setNode(to);
		assertion.getArguments().add(toArgument);
		var value = ProblemFactory.eINSTANCE.createLogicConstant();
		value.setLogicValue(LogicValue.TRUE);
		assertion.setValue(value);
		problem.getStatements().add(assertion);
	}

	private void assertSerializedResult(String expected) {
		String problemString;
		try (var outputStream = new ByteArrayOutputStream()) {
			resource.save(outputStream, Map.of());
			problemString = outputStream.toString();
		} catch (IOException e) {
			throw new AssertionError("Failed to serialize problem", e);
		}
		// Nothing to handle in a test.

		assertThat(problemString.replace("\r\n", "\n"), equalTo(expected.replace("\r\n", "\n")));
	}
}
