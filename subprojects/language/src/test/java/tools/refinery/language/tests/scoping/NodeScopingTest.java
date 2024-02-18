/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.scoping;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.model.tests.utils.WrappedProblem;
import tools.refinery.language.tests.ProblemInjectorProvider;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class NodeScopingTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@ParameterizedTest
	@ValueSource(strings = { "", "builtin::" })
	void builtInArgumentTypeTest(String qualifiedNamePrefix) {
		var problem = parse("""
				pred predicate({PARAM}node a).
				""", qualifiedNamePrefix);
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.pred("predicate").param(0).getParameterType(),
				equalTo(problem.builtin().findClass("node").get()));
	}

	@Test
	void implicitNodeInAssertionTest() {
		var problem = parse("""
				pred predicate(node x, node y) <-> node(x).
				predicate(a, a).
				?predicate(a, b).
				""");
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.nodeNames(), hasItems("a", "b"));
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.node("a")));
		assertThat(problem.assertion(0).arg(1).node(), equalTo(problem.node("a")));
		assertThat(problem.assertion(1).arg(0).node(), equalTo(problem.node("a")));
		assertThat(problem.assertion(1).arg(1).node(), equalTo(problem.node("b")));
	}

	@Test
	void implicitNodeInPredicateTest() {
		var problem = parse("""
				pred predicate(node a) <-> node(b).
				predicate(b).
				""");
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.nodeNames(), hasItems("b"));
		assertThat(problem.pred("predicate").conj(0).lit(0).arg(0).node(), equalTo(problem.node("b")));
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.node("b")));
	}

	@Test
	void atomNodeInAssertionTest() {
		var problem = parse("""
				atom a, b.
				pred predicate(node x, node y) <-> node(x).
				predicate(a, a).
				?predicate(a, b).
				""");
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.atomNode("a")));
		assertThat(problem.assertion(0).arg(1).node(), equalTo(problem.atomNode("a")));
		assertThat(problem.assertion(1).arg(0).node(), equalTo(problem.atomNode("a")));
		assertThat(problem.assertion(1).arg(1).node(), equalTo(problem.atomNode("b")));
	}

	@Test
	void atomNodeInPredicateTest() {
		var problem = parse("""
				atom b.
				pred predicate(node a) <-> node(b).
				""");
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.pred("predicate").conj(0).lit(0).arg(0).node(), equalTo(problem.atomNode("b")));
	}

	@Disabled("No nodes are present in builtin.problem currently")
	@ParameterizedTest
	@MethodSource("builtInNodeReferencesSource")
	void builtInNodeTest(String qualifiedName) {
		var problem = parse("""
				pred predicate(node x) <-> node(x).
				predicate({PARAM}).
				""", qualifiedName);
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.builtin().findClass("int").get().getNewNode()));
	}

	@Disabled("No nodes are present in builtin.problem currently")
	@ParameterizedTest
	@MethodSource("builtInNodeReferencesSource")
	void builtInNodeInPredicateTest(String qualifiedName) {
		var problem = parse("""
				pred predicate(node x) <-> node({PARAM}).
				""", qualifiedName);
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.pred("predicate").conj(0).lit(0).arg(0).node(),
				equalTo(problem.builtin().findClass("int").get().getNewNode()));
	}

	static Stream<Arguments> builtInNodeReferencesSource() {
		return Stream.of(Arguments.of("int::new"), Arguments.of("builtin::int::new"));
	}

	@Test
	void classNewNodeTest() {
		var problem = parse("""
				class Foo.
				pred predicate(node x) <-> node(x).
				predicate(Foo::new).
				""");
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.findClass("Foo").get().getNewNode()));
	}

	@Test
	void classNewNodeInPredicateTest() {
		var problem = parse("""
				class Foo.
				pred predicate(node x) <-> node(Foo::new).
				""");
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.pred("predicate").conj(0).lit(0).arg(0).node(),
				equalTo(problem.findClass("Foo").get().getNewNode()));
	}

	@Test
	void newNodeIsNotSpecial() {
		var problem = parse("""
				class Foo.
				pred predicate(node x) <-> node(x).
				predicate(new).
				""");
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.nodeNames(), hasItems("new"));
		assertThat(problem.assertion(0).arg(0).node(), not(equalTo(problem.findClass("Foo").get().getNewNode())));
	}

	@ParameterizedTest
	@MethodSource("enumLiteralReferencesSource")
	void enumLiteralTest(String qualifiedName) {
		var problem = parse("""
				enum Foo { alpha, beta }
				pred predicate(Foo a) <-> node(a).
				predicate({PARAM}).
				""", qualifiedName);
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.findEnum("Foo").literal("alpha")));
	}

	@ParameterizedTest
	@MethodSource("enumLiteralReferencesSource")
	void enumLiteralInPredicateTest(String qualifiedName) {
		var problem = parse("""
				enum Foo { alpha, beta }
				pred predicate(Foo a) <-> node({PARAM}).
				""", qualifiedName);
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.pred("predicate").conj(0).lit(0).arg(0).node(),
				equalTo(problem.findEnum("Foo").literal("alpha")));
	}

	static Stream<Arguments> enumLiteralReferencesSource() {
		return Stream.of(Arguments.of("alpha"), Arguments.of("Foo::alpha"));
	}

	@Disabled("No enum literals are present in builtin.problem currently")
	@ParameterizedTest
	@MethodSource("builtInEnumLiteralReferencesSource")
	void builtInEnumLiteralTest(String qualifiedName) {
		var problem = parse("""
				pred predicate(node a) <-> node(a).
				predicate({PARAM}).
				""", qualifiedName);
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.assertion(0).arg(0).node(), equalTo(problem.builtin().findEnum("bool").literal("true")));
	}

	@Disabled("No enum literals are present in builtin.problem currently")
	@ParameterizedTest
	@MethodSource("builtInEnumLiteralReferencesSource")
	void builtInEnumLiteralInPredicateTest(String qualifiedName) {
		var problem = parse("""
				pred predicate() <-> node({PARAM}).
				""", qualifiedName);
		assertThat(problem.getResourceErrors(), empty());
		assertThat(problem.nodeNames(), empty());
		assertThat(problem.pred("predicate").conj(0).lit(0).arg(0).node(),
				equalTo(problem.builtin().findEnum("bool").literal("true")));
	}

	static Stream<Arguments> builtInEnumLiteralReferencesSource() {
		return Stream.of(Arguments.of("true"), Arguments.of("bool::true"), Arguments.of("builtin::true"),
				Arguments.of("builtin::bool::true"));
	}

	private WrappedProblem parse(String text, String parameter) {
		return parseHelper.parse(text.replace("{PARAM}", parameter));
	}

	private WrappedProblem parse(String text) {
		return parse(text, "");
	}
}
