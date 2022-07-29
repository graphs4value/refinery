package tools.refinery.language.tests.scoping;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

import java.util.stream.Stream;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.inject.Inject;

import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.model.tests.utils.WrappedProblem;
import tools.refinery.language.tests.ProblemInjectorProvider;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class NodeScopingTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@ValueSource(strings = { "", "builtin::" })
	void builtInArgumentTypeTest(String qualifiedNamePrefix) {
		var problem = parse("""
				pred predicate({PARAM}node a, {PARAM}data b, {PARAM}int c).
				""", qualifiedNamePrefix);
		assertThat(problem.errors(), empty());
		assertThat(problem.pred("predicate").param(0).getParameterType(),
				equalTo(problem.builtin().findClass("node").get()));
		assertThat(problem.pred("predicate").param(1).getParameterType(),
				equalTo(problem.builtin().findClass("data").get()));
		assertThat(problem.pred("predicate").param(2).getParameterType(),
				equalTo(problem.builtin().findClass("int").get()));
	}

	@Test
	void implicitNodeInAssertionTest() {
		var problem = parse("""
				pred predicate(node x, node y) <-> node(x).
				predicate(a, a).
				?predicate(a, b).
				""");
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), hasItems("a", "b"));
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.node("a")));
		assertThat(problem.assertion(0).arg(1).node(), equalTo(problem.node("a")));
		assertThat(problem.assertion(1).arg(0).node(), equalTo(problem.node("a")));
		assertThat(problem.assertion(1).arg(1).node(), equalTo(problem.node("b")));
	}

	@Test
	void implicitNodeInNodeValueAssertionTest() {
		var problem = parseHelper.parse("""
				a: 16.
				""");
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), hasItems("a"));
		assertThat(problem.nodeValueAssertion(0).getNode(), equalTo(problem.node("a")));
	}

	@Test
	void implicitNodeInPredicateTest() {
		var problem = parse("""
				pred predicate(node a) <-> node(b).
				predicate(b).
				""");
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), hasItems("b"));
		assertThat(problem.pred("predicate").conj(0).lit(0).arg(0).node(), equalTo(problem.node("b")));
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.node("b")));
	}

	@ParameterizedTest
	@MethodSource("individualNodeReferenceSource")
	void individualNodeInAssertionTest(String qualifiedNamePrefix, boolean namedProblem) {
		var problem = parse("""
				indiv a, b.
				pred predicate(node x, node y) <-> node(x).
				predicate({PARAM}a, {PARAM}a).
				?predicate({PARAM}a, {PARAM}b).
				""", qualifiedNamePrefix, namedProblem);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.individualNode("a")));
		assertThat(problem.assertion(0).arg(1).node(), equalTo(problem.individualNode("a")));
		assertThat(problem.assertion(1).arg(0).node(), equalTo(problem.individualNode("a")));
		assertThat(problem.assertion(1).arg(1).node(), equalTo(problem.individualNode("b")));
	}

	@ParameterizedTest
	@MethodSource("individualNodeReferenceSource")
	void individualNodeInNodeValueAssertionTest(String qualifiedNamePrefix, boolean namedProblem) {
		var problem = parse("""
				indiv a.
				{PARAM}a: 16.
				""", qualifiedNamePrefix, namedProblem);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.nodeValueAssertion(0).getNode(), equalTo(problem.individualNode("a")));
	}

	@ParameterizedTest
	@MethodSource("individualNodeReferenceSource")
	void individualNodeInPredicateTest(String qualifiedNamePrefix, boolean namedProblem) {
		var problem = parse("""
				indiv b.
				pred predicate(node a) <-> node({PARAM}b).
				""");
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.pred("predicate").conj(0).lit(0).arg(0).node(), equalTo(problem.individualNode("b")));
	}

	static Stream<Arguments> individualNodeReferenceSource() {
		return Stream.of(Arguments.of("", false), Arguments.of("", true), Arguments.of("test::", true));
	}

	@ParameterizedTest
	@MethodSource("builtInNodeReferencesSource")
	void builtInNodeTest(String qualifiedName) {
		var problem = parse("""
				pred predicate(node x) <-> node(x).
				predicate({PARAM}).
				""", qualifiedName);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.builtin().findClass("int").get().getNewNode()));
	}

	@ParameterizedTest
	@MethodSource("builtInNodeReferencesSource")
	void builtInNodeInNodeValueAssertionTest(String qualifiedName) {
		var problem = parse("""
				{PARAM}: 16.
				""", qualifiedName);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.nodeValueAssertion(0).getNode(),
				equalTo(problem.builtin().findClass("int").get().getNewNode()));
	}

	@ParameterizedTest
	@MethodSource("builtInNodeReferencesSource")
	void builtInNodeInPredicateTest(String qualifiedName) {
		var problem = parse("""
				pred predicate(node x) <-> node({PARAM}).
				""", qualifiedName);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.pred("predicate").conj(0).lit(0).arg(0).node(),
				equalTo(problem.builtin().findClass("int").get().getNewNode()));
	}

	static Stream<Arguments> builtInNodeReferencesSource() {
		return Stream.of(Arguments.of("int::new"), Arguments.of("builtin::int::new"));
	}

	@ParameterizedTest
	@MethodSource("classNewNodeReferencesSource")
	void classNewNodeTest(String qualifiedName, boolean namedProblem) {
		var problem = parse("""
				class Foo.
				pred predicate(node x) <-> node(x).
				predicate({PARAM}).
				""", qualifiedName, namedProblem);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.findClass("Foo").get().getNewNode()));
	}

	@ParameterizedTest
	@MethodSource("classNewNodeReferencesSource")
	void classNewNodeInNodeValueAssertionTest(String qualifiedName, boolean namedProblem) {
		var problem = parse("""
				class Foo.
				{PARAM}: 16.
				""", qualifiedName, namedProblem);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.nodeValueAssertion(0).getNode(), equalTo(problem.findClass("Foo").get().getNewNode()));
	}

	@ParameterizedTest
	@MethodSource("classNewNodeReferencesSource")
	void classNewNodeInPredicateTest(String qualifiedName, boolean namedProblem) {
		var problem = parse("""
				class Foo.
				pred predicate(node x) <-> node({PARAM}).
				""", qualifiedName, namedProblem);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.pred("predicate").conj(0).lit(0).arg(0).node(),
				equalTo(problem.findClass("Foo").get().getNewNode()));
	}

	static Stream<Arguments> classNewNodeReferencesSource() {
		return Stream.of(Arguments.of("Foo::new", false), Arguments.of("Foo::new", true),
				Arguments.of("test::Foo::new", true));
	}

	@Test
	void newNodeIsNotSpecial() {
		var problem = parse("""
				class Foo.
				pred predicate(node x) <-> node(x).
				predicate(new).
				""");
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), hasItems("new"));
		assertThat(problem.assertion(0).arg(0).node(), not(equalTo(problem.findClass("Foo").get().getNewNode())));
	}

	@ParameterizedTest
	@MethodSource("enumLiteralReferencesSource")
	void enumLiteralTest(String qualifiedName, boolean namedProblem) {
		var problem = parse("""
				enum Foo { alpha, beta }
				pred predicate(Foo a) <-> node(a).
				predicate({PARAM}).
				""", qualifiedName, namedProblem);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.findEnum("Foo").literal("alpha")));
	}

	@ParameterizedTest
	@MethodSource("enumLiteralReferencesSource")
	void enumLiteralInNodeValueAssertionTest(String qualifiedName, boolean namedProblem) {
		var problem = parse("""
				enum Foo { alpha, beta }
				{PARAM}: 16.
				""", qualifiedName, namedProblem);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.nodeValueAssertion(0).getNode(), equalTo(problem.findEnum("Foo").literal("alpha")));
	}

	@ParameterizedTest
	@MethodSource("enumLiteralReferencesSource")
	void enumLiteralInPredicateTest(String qualifiedName, boolean namedProblem) {
		var problem = parse("""
				enum Foo { alpha, beta }
				pred predicate(Foo a) <-> node({PARAM}).
				""", qualifiedName, namedProblem);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.pred("predicate").conj(0).lit(0).arg(0).node(),
				equalTo(problem.findEnum("Foo").literal("alpha")));
	}

	static Stream<Arguments> enumLiteralReferencesSource() {
		return Stream.of(Arguments.of("alpha", false), Arguments.of("alpha", true), Arguments.of("Foo::alpha", false),
				Arguments.of("Foo::alpha", true), Arguments.of("test::alpha", true),
				Arguments.of("test::Foo::alpha", true));
	}

	@ParameterizedTest
	@MethodSource("builtInEnumLiteralReferencesSource")
	void builtInEnumLiteralTest(String qualifiedName) {
		var problem = parse("""
				pred predicate(node a) <-> node(a).
				predicate({PARAM}).
				""", qualifiedName);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.builtin().findEnum("bool").literal("true")));
	}

	@ParameterizedTest
	@MethodSource("builtInEnumLiteralReferencesSource")
	void builtInEnumLiteralInNodeValueAssertionTest(String qualifiedName) {
		var problem = parse("""
				{PARAM}: 16.
				""", qualifiedName);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.nodeValueAssertion(0).getNode(),
				equalTo(problem.builtin().findEnum("bool").literal("true")));
	}

	@ParameterizedTest
	@MethodSource("builtInEnumLiteralReferencesSource")
	void bultInEnumLiteralInPredicateTest(String qualifiedName) {
		var problem = parse("""
				pred predicate() <-> node({PARAM}).
				""", qualifiedName);
		assertThat(problem.errors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.pred("predicate").conj(0).lit(0).arg(0).node(),
				equalTo(problem.builtin().findEnum("bool").literal("true")));
	}

	static Stream<Arguments> builtInEnumLiteralReferencesSource() {
		return Stream.of(Arguments.of("true"), Arguments.of("bool::true"), Arguments.of("builtin::true"),
				Arguments.of("builtin::bool::true"));
	}

	private WrappedProblem parse(String text, String parameter, boolean namedProblem) {
		var problemName = namedProblem ? "problem test.\n" : "";
		return parseHelper.parse(problemName + text.replace("{PARAM}", parameter));
	}

	private WrappedProblem parse(String text, String parameter) {
		return parse(text, parameter, false);
	}

	private WrappedProblem parse(String text) {
		return parse(text, "");
	}
}
