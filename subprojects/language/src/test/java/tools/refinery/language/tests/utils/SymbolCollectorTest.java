package tools.refinery.language.tests.utils;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.tests.ProblemInjectorProvider;
import tools.refinery.language.utils.ContainmentRole;
import tools.refinery.language.utils.ProblemDesugarer;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class SymbolCollectorTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Inject
	private ProblemDesugarer desugarer;

	@Test
	void implicitNodeTest() {
		var problem = parseHelper.parse("""
				exists(a).
				""");
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		var node = problem.node("a");
		assertThat(collectedSymbols.nodes(), hasKey(node));
		assertThat(collectedSymbols.nodes().get(node).individual(), is(false));
	}

	@Test
	void individualNodeTest() {
		var problem = parseHelper.parse("""
				indiv a.
				""");
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		var node = problem.individualNode("a");
		assertThat(collectedSymbols.nodes(), hasKey(node));
		assertThat(collectedSymbols.nodes().get(node).individual(), is(true));
	}

	@Test
	void classTest() {
		var problem = parseHelper.parse("""
				class Foo.
				""");
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		var classDeclaration = problem.findClass("Foo").get();
		assertThat(collectedSymbols.relations(), hasKey(classDeclaration));
		var classInfo = collectedSymbols.relations().get(classDeclaration);
		assertThat(classInfo.parameters(), hasSize(1));
		assertThat(classInfo.containmentRole(), is(ContainmentRole.CONTAINED));
		assertThat(classInfo.hasDefinition(), is(false));
		var newNode = classDeclaration.getNewNode();
		assertThat(collectedSymbols.nodes(), hasKey(newNode));
		assertThat(collectedSymbols.nodes().get(newNode).individual(), is(false));
		assertThat(classInfo.assertions(), assertsNode(newNode, LogicValue.TRUE));
		assertThat(collectedSymbols.relations().get(problem.builtinSymbols().exists()).assertions(),
				assertsNode(newNode, LogicValue.UNKNOWN));
		assertThat(collectedSymbols.relations().get(problem.builtinSymbols().equals()).assertions(),
				assertsNode(newNode, LogicValue.UNKNOWN));
	}

	@Test
	void abstractClassTest() {
		var problem = parseHelper.parse("""
				abstract class Foo.
				""");
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		assertThat(collectedSymbols.relations().get(problem.findClass("Foo").get()).assertions(), hasSize(0));
	}

	@Test
	void referenceTest() {
		var problem = parseHelper.parse("""
				class Foo {
					Foo[] bar opposite quux
					Foo quux opposite bar
				}
				""");
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		var fooClass = problem.findClass("Foo");
		var barReference = fooClass.reference("bar");
		var barInfo = collectedSymbols.relations().get(barReference);
		var quuxReference = fooClass.reference("quux");
		var quuxInfo = collectedSymbols.relations().get(quuxReference);
		assertThat(barInfo.containmentRole(), is(ContainmentRole.NONE));
		assertThat(barInfo.opposite(), is(quuxReference));
		assertThat(barInfo.multiplicity(), is(instanceOf(UnboundedMultiplicity.class)));
		assertThat(barInfo.hasDefinition(), is(false));
		assertThat(quuxInfo.containmentRole(), is(ContainmentRole.NONE));
		assertThat(quuxInfo.opposite(), is(barReference));
		assertThat(quuxInfo.multiplicity(), is(instanceOf(ExactMultiplicity.class)));
		assertThat(quuxInfo.multiplicity(), hasProperty("exactValue", is(1)));
		assertThat(quuxInfo.hasDefinition(), is(false));
	}

	@Test
	void containmentReferenceTest() {
		var problem = parseHelper.parse("""
				class Foo {
					contains Foo[] bar
				}
				""");
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		assertThat(collectedSymbols.relations().get(problem.findClass("Foo").reference("bar")).containmentRole(),
				is(ContainmentRole.CONTAINMENT));
	}

	@Test
	void dataReferenceTest() {
		var problem = parseHelper.parse("""
				class Foo {
					int bar
				}
				""");
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		assertThat(collectedSymbols.relations().get(problem.findClass("Foo").reference("bar")).containmentRole(),
				is(ContainmentRole.CONTAINMENT));
	}

	@Test
	void enumTest() {
		var problem = parseHelper.parse("""
				enum Foo {
					bar, quux
				}
				""");
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		var enumDeclaration = problem.findEnum("Foo");
		var enumInfo = collectedSymbols.relations().get(enumDeclaration.get());
		assertThat(enumInfo.containmentRole(), is(ContainmentRole.NONE));
		assertThat(enumInfo.assertions(), assertsNode(enumDeclaration.literal("bar"), LogicValue.TRUE));
		assertThat(enumInfo.assertions(), assertsNode(enumDeclaration.literal("quux"), LogicValue.TRUE));
	}

	@ParameterizedTest
	@MethodSource
	void predicateTest(String keyword, ContainmentRole containmentRole) {
		var problem = parseHelper.parse(keyword + " foo(node x) <-> domain(x); data(x).");
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		var predicateInfo = collectedSymbols.relations().get(problem.pred("foo").get());
		assertThat(predicateInfo.containmentRole(), is(containmentRole));
		assertThat(predicateInfo.parameters(), hasSize(1));
		assertThat(predicateInfo.bodies(), hasSize(2));
		assertThat(predicateInfo.hasDefinition(), is(true));
	}

	static Stream<Arguments> predicateTest() {
		return Stream.of(Arguments.of("pred", ContainmentRole.NONE), Arguments.of("error", ContainmentRole.NONE),
				Arguments.of("contained", ContainmentRole.CONTAINED), Arguments.of("containment",
						ContainmentRole.CONTAINMENT));
	}

	@ParameterizedTest
	@MethodSource("logicValues")
	void assertionTest(String keyword, LogicValue value) {
		var problem = parseHelper.parse("""
				pred foo(node x).
				foo(a): %s.
				""".formatted(keyword));
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		assertThat(collectedSymbols.relations().get(problem.pred("foo").get()).assertions(),
				assertsNode(problem.node("a"), value));
	}

	@ParameterizedTest
	@MethodSource("logicValues")
	void defaultAssertionTest(String keyword, LogicValue value) {
		var problem = parseHelper.parse("""
				pred foo(node x).
				default foo(a): %s.
				""".formatted(keyword));
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		assertThat(collectedSymbols.relations().get(problem.pred("foo").get()).assertions(),
				assertsNode(problem.node("a"), value));
	}

	static Stream<Arguments> logicValues() {
		return Stream.of(Arguments.of("true", LogicValue.TRUE), Arguments.of("false", LogicValue.FALSE),
				Arguments.of("unknown", LogicValue.UNKNOWN), Arguments.of("error", LogicValue.ERROR));
	}

	@Test
	void invalidAssertionArityTest() {
		var problem = parseHelper.parse("""
				pred foo(node x).
				foo(a, b).
				""");
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		assertThat(collectedSymbols.relations().get(problem.pred("foo").get()).assertions(), hasSize(0));
	}

	@ParameterizedTest
	@MethodSource("valueTypes")
	void nodeValueAssertionTest(String value, String typeName) {
		var problem = parseHelper.parse("a: %s.".formatted(value));
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		var node = problem.node("a");
		var nodeInfo = collectedSymbols.nodes().get(node);
		assertThat(nodeInfo.individual(), is(false));
		assertThat(nodeInfo.valueAssertions(), hasSize(1));
		assertThat(collectedSymbols.relations().get(problem.builtin().findClass(typeName).get()).assertions(),
				assertsNode(node, LogicValue.TRUE));
	}

	@ParameterizedTest
	@MethodSource("valueTypes")
	void constantInAssertionTest(String value, String typeName) {
		var problem = parseHelper.parse("""
				containment pred foo(node x, data y).
				foo(a, %s).
				""".formatted(value));
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		var node = problem.assertion(0).arg(1).constantNode();
		var nodeInfo = collectedSymbols.nodes().get(node);
		assertThat(nodeInfo.individual(), is(false));
		assertThat(nodeInfo.valueAssertions(), hasSize(1));
		assertThat(collectedSymbols.relations().get(problem.pred("foo").get()).assertions(), assertsNode(node,
				LogicValue.TRUE));
		assertThat(collectedSymbols.relations().get(problem.builtin().findClass(typeName).get()).assertions(),
				assertsNode(node, LogicValue.TRUE));
	}

	@ParameterizedTest
	@MethodSource("valueTypes")
	void constantInUnknownAssertionTest(String value, String typeName) {
		var problem = parseHelper.parse("""
				containment pred foo(node x, data y).
				foo(a, %s): unknown.
				""".formatted(value));
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		var node = problem.assertion(0).arg(1).constantNode();
		var nodeInfo = collectedSymbols.nodes().get(node);
		assertThat(nodeInfo.individual(), is(false));
		assertThat(nodeInfo.valueAssertions(), hasSize(1));
		assertThat(collectedSymbols.relations().get(problem.pred("foo").get()).assertions(), assertsNode(node,
				LogicValue.UNKNOWN));
		assertThat(collectedSymbols.relations().get(problem.builtin().findClass(typeName).get()).assertions(),
				assertsNode(node, LogicValue.TRUE));
		assertThat(collectedSymbols.relations().get(problem.builtinSymbols().exists()).assertions(), assertsNode(node,
				LogicValue.UNKNOWN));
	}

	static Stream<Arguments> valueTypes() {
		return Stream.of(Arguments.of("3", "int"), Arguments.of("3.14", "real"), Arguments.of("\"foo\"", "string"));
	}

	@Test
	void invalidProblemTest() {
		var problem = parseHelper.parse("""
				class Foo {
					bar[] opposite quux
					Foo quux opposite bar
				}
				""").get();
		assertDoesNotThrow(() -> desugarer.collectSymbols(problem));
	}

	@Test
	void errorAssertionTest() {
		var problem = parseHelper.parse("""
				error foo(node a, node b) <-> equals(a, b).
				""");
		var collectedSymbols = desugarer.collectSymbols(problem.get());
		var fooInfo = collectedSymbols.relations().get(problem.pred("foo").get());
		assertThat(fooInfo.assertions(), hasSize(1));
		var assertion = fooInfo.assertions().stream().findFirst().orElseThrow();
		assertThat(assertion.getValue(), is(LogicValue.FALSE));
		assertThat(assertion.getArguments(), hasSize(2));
		assertThat(assertion.getArguments(), everyItem(instanceOf(WildcardAssertionArgument.class)));
	}

	private static Matcher<Iterable<? super Assertion>> assertsNode(Node node, LogicValue value) {
		return hasItem(allOf(hasProperty("arguments", hasItem(hasProperty("node", is(node)))), hasProperty("value",
				is(value))));
	}
}
