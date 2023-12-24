/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;


import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.language.tests.ProblemInjectorProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class ProblemLoaderTest {
	private static final String PREFIX = """
			class Foo.
			class Bar.
			""";

	@Inject
	private ProblemLoader loader;

	@ParameterizedTest
	@MethodSource
	void loadScopeConstraintsTest(String originalScopes, List<String> scopes, List<String> overrideScopes,
								  String expectedScopes) throws IOException {
		var problem = loader.loadString(PREFIX + originalScopes);
		var modifiedProblem = loader.loadScopeConstraints(problem, scopes, overrideScopes);
		String serializedProblem;
		try (var outputStream = new ByteArrayOutputStream()) {
			modifiedProblem.eResource().save(outputStream, Map.of());
			serializedProblem = outputStream.toString(StandardCharsets.UTF_8);
		}
		assertThat(serializedProblem, is(PREFIX + expectedScopes));
	}

	static Stream<Arguments> loadScopeConstraintsTest() {
		return Stream.of(Arguments.of("",
				List.of(),
				List.of(),
				""), Arguments.of("",
				List.of("node=5..10"),
				List.of(), """

						scope node=5..10.
						"""), Arguments.of("",
				List.of("Foo=2", "Bar=3"),
				List.of(), """

						scope Foo=2.
						scope Bar=3.
						"""), Arguments.of("""
						scope Foo = 1, Bar = 1.
						""",
				List.of("node=5..10"),
				List.of(), """
						scope Foo = 1, Bar = 1.

						scope node=5..10.
						"""), Arguments.of("""
						scope Foo = 0..10, Bar = 1.
						""",
				List.of("Foo = 5"),
				List.of(), """
						scope Foo = 0..10, Bar = 1.

						scope Foo = 5.
						"""), Arguments.of("""
						scope Foo = 1, Bar = 1.
						""",
				List.of(),
				List.of("node=5..10"), """
						scope Foo = 1, Bar = 1.

						scope node=5..10.
						"""), Arguments.of("""
						scope Foo = 1, Bar = 1.
						""",
				List.of(),
				List.of("Foo=3..4"), """
						scope Bar = 1.

						scope Foo=3..4.
						"""), Arguments.of("""
						scope Foo = 1, Bar = 1.
						""",
				List.of("Foo=2"),
				List.of("Foo=3..4"), """
						scope Bar = 1.

						scope Foo=2.
						scope Foo=3..4.
						"""), Arguments.of("""
						scope Foo = 1.
						scope Bar = 1.
						""",
				List.of(),
				List.of("Bar=3..4"), """
						scope Foo = 1.


						scope Bar=3..4.
						"""), Arguments.of("""
						scope Foo = 1, Bar = 1.
						""",
				List.of(),
				List.of("Foo=3..4", "Bar=4..5"), """

						scope Foo=3..4.
						scope Bar=4..5.
						"""));
	}
}
