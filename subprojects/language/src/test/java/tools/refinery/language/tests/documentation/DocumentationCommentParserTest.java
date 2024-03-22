/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.documentation;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.language.documentation.DocumentationCommentParser;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.tests.ProblemInjectorProvider;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class DocumentationCommentParserTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Inject
	private DocumentationCommentParser commentParser;

	@ParameterizedTest
	@MethodSource
	void colorTest(String text, String expectedColor) {
		var parseResult = parseHelper.parse(text);
		var foo = parseResult.findClass("Foo").classDeclaration();
		var documentation = commentParser.parseDocumentation(foo);
		var actualColor = documentation.get(DocumentationCommentParser.COLOR_TAG);
		assertThat(actualColor, is(expectedColor));
	}

	static Stream<Arguments> colorTest() {
		return Stream.of(
				Arguments.of("class Foo.", null),
				Arguments.of("""
						% @color #ff0000
						class Foo.
						""", null),
				Arguments.of("""
						/*
						 * @color #ff0000
						 */
						class Foo.
						""", null),
				Arguments.of("""
						/**
						 * @color #ff0000
						 */
						class Foo.
						""", "_ff0000"),
				Arguments.of("""
						/**
						 * @color #ff0000 other
						 */
						class Foo.
						""", "_ff0000"),
				Arguments.of("""
						/** @color #ff0000 */
						class Foo.
						""", "_ff0000"),
				Arguments.of("""
						/**@color #ff0000*/
						class Foo.
						""", "_ff0000"),
				Arguments.of("""
						/**@color\t  #ff0000*/
						class Foo.
						""", "_ff0000"),
				Arguments.of("""
						/** @color #F2af00 */
						class Foo.
						""", "_f2af00"),
				Arguments.of("""
						/** @color #Fa0 */
						class Foo.
						""", "_fa0"),
				Arguments.of("""
						/** @color 4 */
						class Foo.
						""", "4")
		);
	}
}
