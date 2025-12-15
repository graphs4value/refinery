/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.annotations;

import com.google.inject.Inject;
import org.eclipse.xtext.naming.QualifiedName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.language.annotations.AnnotationContext;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.NodeDeclaration;
import tools.refinery.language.tests.InjectWithRefinery;
import tools.refinery.language.tests.utils.ProblemParseHelper;
import tools.refinery.language.tests.utils.WrappedProblem;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

// This test uses {@code Optional} arguments as expected values of parameterized tests.
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@InjectWithRefinery
class AnnotationContextTest {
	private static final QualifiedName EXAMPLE = QualifiedName.create("Example");

	@Inject
	ProblemParseHelper parseHelper;

	@Inject
	AnnotationContext annotationContext;

	@Test
	void nullAnnotationTest() {
		assertThrows(IllegalArgumentException.class, () -> annotationContext.annotationsFor(null));
	}

	@Test
	void rootAnnotationTest() {
		var problem = parseHelper.parse("""
				#pred Example().

				#Example.
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		assertThat(annotations.getAllAnnotations().toList(), hasSize(1));
		assertThat(annotations.hasAnnotation(EXAMPLE), is(true));
		assertThat(annotations.getAnnotation(EXAMPLE).isPresent(), is(true));
		assertThat(annotations.getAnnotations(EXAMPLE).toList(), hasSize(1));
	}

	@Test
	void nonExistingAnnotationTest() {
		var problem = parseHelper.parse("""
				class Example.
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		assertThat(annotations.getAllAnnotations().toList(), is(empty()));
		assertThat(annotations.hasAnnotation(EXAMPLE), is(false));
		assertThat(annotations.getAnnotation(EXAMPLE).isPresent(), is(false));
		assertThat(annotations.getAnnotations(EXAMPLE).toList(), is(empty()));
	}

	@Test
	void repeatableAnnotationTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				@repeatable
				#pred Example().

				#Example.
				#Example.
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		assertThat(annotations.getAllAnnotations().toList(), hasSize(2));
		assertThat(annotations.hasAnnotation(EXAMPLE), is(true));
		assertThat(annotations.getAnnotations(EXAMPLE).toList(), hasSize(2));
		assertThrows(IllegalArgumentException.class, () -> annotations.getAnnotation(EXAMPLE));
	}

	@Test
	void singleRepeatableAnnotationTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				@repeatable
				#pred Example().

				#Example.
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		assertThrows(IllegalArgumentException.class, () -> annotations.getAnnotation(EXAMPLE));
	}

	@Test
	void zeroRepeatableAnnotationTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				@repeatable
				#pred Example().
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		// We do not throw an exception if the annotation doesn't appear at all.
		assertThat(annotations.getAnnotation(EXAMPLE).isPresent(), is(false));
	}

	@Test
	void classAnnotationTest() {
		var problem = parseHelper.parse("""
				#pred Example().

				@Example
				class Person.
				""");
		var annotations = annotationContext.annotationsFor(problem.findClass("Person").get());
		assertThat(annotations.hasAnnotation(EXAMPLE), is(true));
	}

	@Test
	void referenceAnnotationTest() {
		var problem = parseHelper.parse("""
				#pred Example().

				class Person {
					@Example
				    Person[] friend
				}
				""");
		var annotations = annotationContext.annotationsFor(problem.findClass("Person").feature("friend"));
		assertThat(annotations.hasAnnotation(EXAMPLE), is(true));
	}

	@Test
	void enumAnnotationTest() {
		var problem = parseHelper.parse("""
				#pred Example().

				@Example
				enum Color {
				    RED,
					GREEN,
					BLUE
				}
				""");
		var annotations = annotationContext.annotationsFor(problem.findEnum("Color").get());
		assertThat(annotations.hasAnnotation(EXAMPLE), is(true));
	}

	@Test
	void enumLiteralAnnotationTest() {
		var problem = parseHelper.parse("""
				#pred Example().

				enum Color {
				    RED,
					@Example
					GREEN,
					BLUE
				}
				""");
		var literals = problem.findEnum("Color").get().getLiterals();
		var annotations1 = annotationContext.annotationsFor(literals.get(0));
		assertThat(annotations1.hasAnnotation(EXAMPLE), is(false));
		var annotations2 = annotationContext.annotationsFor(literals.get(1));
		assertThat(annotations2.hasAnnotation(EXAMPLE), is(true));
	}

	@Test
	void predAnnotationTest() {
		var problem = parseHelper.parse("""
				#pred Example().

				@Example
				pred friend(p1, p2).
				""");
		var annotations = annotationContext.annotationsFor(problem.pred("friend").get());
		assertThat(annotations.hasAnnotation(EXAMPLE), is(true));
	}

	@Test
	void parameterAnnotationTest() {
		var problem = parseHelper.parse("""
				#pred Example().

				pred friend(@Example p1, p2).
				""");
		var parameters = problem.pred("friend").get().getParameters();
		var annotations1 = annotationContext.annotationsFor(parameters.get(0));
		assertThat(annotations1.hasAnnotation(EXAMPLE), is(true));
		var annotations2 = annotationContext.annotationsFor(parameters.get(1));
		assertThat(annotations2.hasAnnotation(EXAMPLE), is(false));
	}

	@Test
	void atomDeclarationAnnotationTest() {
		var problem = parseHelper.parse("""
				#pred Example().

				@Example
				atom foo, bar.
				""");
		var declaration = getNodeDeclaration(problem);
		var atom1 = declaration.getNodes().get(0);
		var atom2 = declaration.getNodes().get(1);
		// Annotations of the atom declaration are inherited by the declared atoms.
		var annotations1 = annotationContext.annotationsFor(atom1);
		assertThat(annotations1.hasAnnotation(EXAMPLE), is(true));
		assertThat(annotations1.getAnnotation(EXAMPLE).orElseThrow().getAnnotatedElement(), is(atom1));
		var annotations2 = annotationContext.annotationsFor(atom2);
		assertThat(annotations2.hasAnnotation(EXAMPLE), is(true));
		assertThat(annotations2.getAnnotation(EXAMPLE).orElseThrow().getAnnotatedElement(), is(atom2));
		// Annotations of the atom declaration should not apply to the declaration itself.
		var annotations3 = annotationContext.annotationsFor(declaration);
		assertThat(annotations3.hasAnnotation(EXAMPLE), is(false));
	}

	private static NodeDeclaration getNodeDeclaration(WrappedProblem problem) {
		return (NodeDeclaration) problem.get().getStatements().stream()
				.filter(NodeDeclaration.class::isInstance)
				.findFirst()
				.orElseThrow();
	}

	@Test
	void atomAnnotationTest() {
		var problem = parseHelper.parse("""
				#pred Example().

				atom @Example foo, bar.
				""");
		var declaration = getNodeDeclaration(problem);
		var atom1 = declaration.getNodes().get(0);
		var atom2 = declaration.getNodes().get(1);
		var annotations1 = annotationContext.annotationsFor(atom1);
		assertThat(annotations1.hasAnnotation(EXAMPLE), is(true));
		var annotations2 = annotationContext.annotationsFor(atom2);
		assertThat(annotations2.hasAnnotation(EXAMPLE), is(false));
	}

	@Test
	void atomAndDeclarationAnnotationTest() {
		var problem = parseHelper.parse("""
				#pred Example().
				#pred Other().

				@Example
				atom @Other foo, bar.
				""");
		var declaration = getNodeDeclaration(problem);
		var atom1 = declaration.getNodes().get(0);
		var atom2 = declaration.getNodes().get(1);
		var annotations1 = annotationContext.annotationsFor(atom1);
		assertThat(annotations1.hasAnnotation(EXAMPLE), is(true));
		var otherName = QualifiedName.create("Other");
		assertThat(annotations1.hasAnnotation(otherName), is(true));
		var annotations2 = annotationContext.annotationsFor(atom2);
		assertThat(annotations2.hasAnnotation(EXAMPLE), is(true));
		assertThat(annotations2.hasAnnotation(otherName), is(false));
	}

	@Test
	void enumLiteralNodeArgumentTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				#pred Example(Color value).

				enum Color { RED, GREEN, BLUE }

				#Example(GREEN).
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		var literal = problem.findEnum("Color").literal("GREEN");
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getNode("value");
		assertThat(actualValue, is(Optional.of(literal)));
	}

	@Test
	void atomNodeArgumentTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				#pred Example(value).

				atom a.

				#Example(a).
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		var declaration = getNodeDeclaration(problem);
		var atom = declaration.getNodes().getFirst();
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getNode("value");
		assertThat(actualValue, is(Optional.of(atom)));
	}

	@Test
	void invalidNodeArgumentTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				#pred Example(value).

				#Example(3).
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getNode("value");
		assertThat(actualValue, is(Optional.empty()));
	}

	@Test
	void optionalNodeArgumentTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				#pred Example(@optional value).

				#Example().
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getNode("value");
		assertThat(actualValue, is(Optional.empty()));
	}

	@Test
	void repeatableNodeArgumentTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				#pred Example(@repeatable value).

				atom a, b, c.

				#Example(b, a, "not a node", b).
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		var declaration = getNodeDeclaration(problem);
		var atom1 = declaration.getNodes().get(0);
		var atom2 = declaration.getNodes().get(1);
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getNodes("value")
				.toArray(Node[]::new);
		assertThat(actualValue, is(new Node[]{atom2, atom1, atom2}));
	}

	@ParameterizedTest
	@MethodSource
	void booleanArgumentTest(String valueString, Optional<Boolean> expectedValue) {
		var problem = parseHelper.parse("""
				#pred Example(bool value).

				#Example(%s).
				""".formatted(valueString));
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getBoolean("value");
		assertThat(actualValue, is(expectedValue));
	}

	static Stream<Arguments> booleanArgumentTest() {
		return Stream.of(
				Arguments.of("true", Optional.of(true)),
				Arguments.of("false", Optional.of(false)),
				// Four-valued logic values are not supported by Java {@code Boolean}.
				Arguments.of("unknown", Optional.empty()),
				Arguments.of("\"not an boolean\"", Optional.empty())
		);
	}

	@Test
	void optionalBooleanArgumentTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				#pred Example(@optional bool value).

				#Example().
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getBoolean("value");
		assertThat(actualValue, is(Optional.empty()));
	}

	@Test
	void repeatableBooleanArgumentTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				#pred Example(@repeatable bool value).

				#Example(true, false, "not a boolean", true).
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getBooleans("value")
				.toArray(Boolean[]::new);
		assertThat(actualValue, is(new Boolean[]{true, false, true}));
	}

	@ParameterizedTest
	@MethodSource
	void intArgumentTest(String valueString, Optional<BigInteger> expectedValue) {
		var problem = parseHelper.parse("""
				#pred Example(int value).

				#Example(%s).
				""".formatted(valueString));
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getBigInteger("value");
		assertThat(actualValue, is(expectedValue));
	}

	static Stream<Arguments> intArgumentTest() {
		return Stream.of(
				Arguments.of("3", Optional.of(BigInteger.valueOf(3))),
				Arguments.of("+3", Optional.of(BigInteger.valueOf(3))),
				Arguments.of("-3", Optional.of(BigInteger.valueOf(-3))),
				Arguments.of("\"not an int\"", Optional.empty())
		);
	}

	@Test
	void optionalIntArgumentTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				#pred Example(@optional int value).

				#Example().
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getBigInteger("value");
		assertThat(actualValue, is(Optional.empty()));
	}

	@Test
	void repeatableIntArgumentTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				#pred Example(@repeatable int value).

				#Example(1, 2, "not an int", 3).
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getBigIntegers("value")
				.toArray();
		assertThat(actualValue, is(new BigInteger[]{BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3)}));
	}

	@ParameterizedTest
	@MethodSource
	void doubleArgumentTest(String valueString, Optional<BigDecimal> expectedValue) {
		var problem = parseHelper.parse("""
				#pred Example(real value).

				#Example(%s).
				""".formatted(valueString));
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getBigDecimal("value");
		assertThat(actualValue, is(expectedValue));
	}

	static Stream<Arguments> doubleArgumentTest() {
		return Stream.of(
				Arguments.of("3.14", Optional.of(new BigDecimal("3.14"))),
				Arguments.of("+3.14", Optional.of(new BigDecimal("3.14"))),
				Arguments.of("-3.14", Optional.of(new BigDecimal("-3.14"))),
				// Implicit coercion of int to real is not supported.
				Arguments.of("3", Optional.empty()),
				Arguments.of("\"not a double\"", Optional.empty())
		);
	}

	@Test
	void optionalBigDecimalArgumentTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				#pred Example(@optional real value).

				#Example().
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getBigDecimal("value");
		assertThat(actualValue, is(Optional.empty()));
	}

	@Test
	void repeatableBigDecimalArgumentTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				#pred Example(@repeatable real value).

				#Example(1.0, 2.0, "not a double", 3.14).
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getBigDecimals("value")
				.toArray();
		assertThat(actualValue, is(new BigDecimal[]{new BigDecimal("1.0"), new BigDecimal("2.0"),
				new BigDecimal("3.14")}));
	}

	@ParameterizedTest
	@MethodSource
	void stringArgumentTest(String valueString, Optional<String> expectedValue) {
		var problem = parseHelper.parse("""
				#pred Example(string value).

				#Example(%s).
				""".formatted(valueString));
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getString("value");
		assertThat(actualValue, is(expectedValue));
	}

	static Stream<Arguments> stringArgumentTest() {
		return Stream.of(
				Arguments.of("\"a a\"", Optional.of("a a")),
				Arguments.of("\"\"", Optional.of("")),
				Arguments.of("3", Optional.empty())
		);
	}

	@Test
	void optionalStringArgumentTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				#pred Example(@optional string value).

				#Example().
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getString("value");
		assertThat(actualValue, is(Optional.empty()));
	}

	@Test
	void repeatableStringArgumentTest() {
		var problem = parseHelper.parse("""
				import builtin::annotations.

				#pred Example(@repeatable string value).

				#Example("a a", "b b", 3, "c c").
				""");
		var annotations = annotationContext.annotationsFor(problem.get());
		var actualValue = annotations.getAnnotation(EXAMPLE)
				.orElseThrow()
				.getStrings("value")
				.toArray(String[]::new);
		assertThat(actualValue, is(new String[]{"a a", "b b", "c c"}));
	}
}
