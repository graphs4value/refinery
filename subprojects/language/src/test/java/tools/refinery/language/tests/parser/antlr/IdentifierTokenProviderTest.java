/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.parser.antlr;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.language.parser.antlr.IdentifierTokenProvider;
import tools.refinery.language.parser.antlr.internal.InternalProblemParser;
import tools.refinery.language.tests.ProblemInjectorProvider;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class IdentifierTokenProviderTest {
	@Inject
	private IdentifierTokenProvider identifierTokenProvider;

	@ParameterizedTest(name = "{0} is identifier: {2}")
	@MethodSource
	void isIdentifierTokenTest(String ignoredTokenName, int tokenId, boolean expected) {
		assertThat(identifierTokenProvider.isIdentifierToken(tokenId), equalTo(expected));
	}

	static Stream<Arguments> isIdentifierTokenTest() {
		return Stream.of(
				Arguments.of("RULE_ID", InternalProblemParser.RULE_ID, true),
				Arguments.of("contained", InternalProblemParser.Contained, true),
				Arguments.of("contains", InternalProblemParser.Contains, true),
				Arguments.of("(", InternalProblemParser.LeftParenthesis, false)
		);
	}
}
