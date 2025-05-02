/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.validation;

import com.google.inject.Inject;
import org.junit.jupiter.api.Test;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;
import tools.refinery.language.tests.InjectWithRefinery;
import tools.refinery.language.tests.utils.ProblemParseHelper;
import tools.refinery.language.validation.ClassHierarchyCollector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;

@InjectWithRefinery
class ClassHierarchyCollectorTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Inject
	private ClassHierarchyCollector classHierarchyCollector;

	@Inject
	private ImportAdapterProvider importAdapterProvider;

	@Test
	void linearInheritanceTest() {
		var problem = parseHelper.parse("""
				class Foo extends Bar.

				class Bar extends Baz.

				class Baz.
				""");
		var nodeSymbol = importAdapterProvider.getBuiltinSymbols(problem.get()).node();
		var foo = problem.findClass("Foo").get();
		var bar = problem.findClass("Bar").get();
		var baz = problem.findClass("Baz").get();
		assertAll(
				() -> assertThat("Foo", classHierarchyCollector.getSuperTypes(foo),
						containsInAnyOrder(bar, baz, nodeSymbol)),
				() -> assertThat("Bar", classHierarchyCollector.getSuperTypes(bar),
						containsInAnyOrder(baz, nodeSymbol)),
				() -> assertThat("Baz", classHierarchyCollector.getSuperTypes(baz),
						containsInAnyOrder(nodeSymbol))
		);
	}

	@Test
	void multipleInheritanceTest() {
		var problem = parseHelper.parse("""
				class Foo extends Bar, Quux.

				class Bar extends Baz, Quux.

				class Baz.

				class Quux.
				""");
		var nodeSymbol = importAdapterProvider.getBuiltinSymbols(problem.get()).node();
		var foo = problem.findClass("Foo").get();
		var bar = problem.findClass("Bar").get();
		var baz = problem.findClass("Baz").get();
		var quux = problem.findClass("Quux").get();
		assertAll(
				() -> assertThat("Foo", classHierarchyCollector.getSuperTypes(foo),
						containsInAnyOrder(bar, baz, quux, nodeSymbol)),
				() -> assertThat("Bar", classHierarchyCollector.getSuperTypes(bar),
						containsInAnyOrder(baz, quux, nodeSymbol)),
				() -> assertThat("Baz", classHierarchyCollector.getSuperTypes(baz),
						containsInAnyOrder(nodeSymbol)),
				() -> assertThat("Quux", classHierarchyCollector.getSuperTypes(quux),
						containsInAnyOrder(nodeSymbol))
		);
	}

	@Test
	void circularInheritanceTest() {
		var problem = parseHelper.parse("""
				class Foo extends Bar.

				class Bar extends Baz.

				class Baz extends Foo.
				""");
		var nodeSymbol = importAdapterProvider.getBuiltinSymbols(problem.get()).node();
		var foo = problem.findClass("Foo").get();
		var bar = problem.findClass("Bar").get();
		var baz = problem.findClass("Baz").get();
		assertAll(
				() -> assertThat("Foo", classHierarchyCollector.getSuperTypes(foo),
						containsInAnyOrder(foo, bar, baz, nodeSymbol)),
				() -> assertThat("Bar", classHierarchyCollector.getSuperTypes(bar),
						containsInAnyOrder(foo, bar, baz, nodeSymbol)),
				() -> assertThat("Baz", classHierarchyCollector.getSuperTypes(baz),
						containsInAnyOrder(foo, bar, baz, nodeSymbol))
		);
	}

	@Test
	void circularMultipleInheritanceTest() {
		var problem = parseHelper.parse("""
				class Foo extends Bar, Quux.

				class Bar extends Baz, Quux.

				class Baz extends Foo.

				class Quux.
				""");
		var nodeSymbol = importAdapterProvider.getBuiltinSymbols(problem.get()).node();
		var foo = problem.findClass("Foo").get();
		var bar = problem.findClass("Bar").get();
		var baz = problem.findClass("Baz").get();
		var quux = problem.findClass("Quux").get();
		assertAll(
				() -> assertThat("Foo", classHierarchyCollector.getSuperTypes(foo),
						containsInAnyOrder(foo, bar, baz, quux, nodeSymbol)),
				() -> assertThat("Bar", classHierarchyCollector.getSuperTypes(bar),
						containsInAnyOrder(foo, bar, baz, quux, nodeSymbol)),
				() -> assertThat("Baz", classHierarchyCollector.getSuperTypes(baz),
						containsInAnyOrder(foo, bar, baz, quux, nodeSymbol)),
				() -> assertThat("Quux", classHierarchyCollector.getSuperTypes(quux),
						containsInAnyOrder(nodeSymbol))
		);
	}


	@Test
	void explicitInheritanceFromNodeTest() {
		var problem = parseHelper.parse("""
				class Foo extends node.
				""");
		var nodeSymbol = importAdapterProvider.getBuiltinSymbols(problem.get()).node();
		var foo = problem.findClass("Foo").get();
		assertThat(classHierarchyCollector.getSuperTypes(foo), containsInAnyOrder(nodeSymbol));
	}
}
