/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.tests.parser.antlr;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.Token;
import org.eclipse.xtext.parser.antlr.Lexer;
import org.eclipse.xtext.parser.antlr.LexerBindings;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.language.parser.antlr.IdentifierTokenProvider;
import tools.refinery.language.parser.antlr.ProblemTokenSource;
import tools.refinery.language.parser.antlr.internal.InternalProblemParser;
import tools.refinery.language.tests.ProblemInjectorProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
class ProblemTokenSourceTest {
	@Inject
	@Named(LexerBindings.RUNTIME)
	private Provider<Lexer> lexerProvider;

	@Inject
	private IdentifierTokenProvider identifierTokenProvider;

	@ParameterizedTest
	@ValueSource(strings = {
			"a+b",
			"a+(b)",
			"a+(b(x, y), x)",
			"a + (b)",
			"a+(b::x)",
			"c+(a+(b)",
			"a+(1, b)",
			// These are never valid expressions, so we do try to peek at the inner plus sign
			// to limit recursion depth in the token source:
			"c+(equals+(a, b))",
			"equals+(equals+(a, b), c)",
	})
	void plusSignInTokenStreamTest(String text) {
		var tokenList = createTokenList(text);
		assertThat(tokenList, hasTokenOfType(InternalProblemParser.PlusSign));
		assertThat(tokenList, not(hasTokenOfType(InternalProblemParser.RULE_TRANSITIVE_CLOSURE)));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"equals+(a, b)",
			"equals + (a, b)",
			"equals+(a::x, b)"
	})
	void transitiveClosureInTokenStreamTest(String text) {
		var tokenList = createTokenList(text);
		assertThat(tokenList, not(hasTokenOfType(InternalProblemParser.PlusSign)));
		assertThat(tokenList, hasTokenOfType(InternalProblemParser.RULE_TRANSITIVE_CLOSURE));
	}

	@ParameterizedTest
	@MethodSource
	void plusAndTransitiveClosureInSameTokenStreamTest(String text, boolean recursive) {
		var tokenSource = createTokenSource(text);
		tokenSource.setRecursive(recursive);
		Token token;
		int i = 0;
		int plusIndex = -1;
		int transitiveClosureIndex = -1;
		do {
			token = tokenSource.nextToken();
			switch (token.getType()) {
			case InternalProblemParser.PlusSign -> {
				assertThat("multiple plus signs", plusIndex, equalTo(-1));
				plusIndex = i;
			}
			case InternalProblemParser.RULE_TRANSITIVE_CLOSURE -> {
				assertThat("multiple transitive closures", transitiveClosureIndex, equalTo(-1));
				transitiveClosureIndex = i;
			}
			}
			i++;
		} while (token.getType() != InternalProblemParser.EOF);
		assertThat("no plus sign", plusIndex, not(equalTo(-1)));
		assertThat("no transitive closure", transitiveClosureIndex, not(equalTo(-1)));
		assertThat("transitive closure before plus", transitiveClosureIndex, greaterThan(plusIndex));
	}

	static Stream<Arguments> plusAndTransitiveClosureInSameTokenStreamTest() {
		return Stream.of(
				Arguments.of("c+(d), equals+(a, b)", false),
				Arguments.of("foo+(bar baz+(a, b))", false),
				// Here we can peek at the inner plus sign without recursion:
				Arguments.of("c+(1, equals+(a, b))", false),
				// But these cases need recursion:
				Arguments.of("c+(equals+(a, b))", true),
				Arguments.of("equals+(equals+(a, b), c)", true)
		);
	}

	private ProblemTokenSource createTokenSource(String text) {
		var lexer = lexerProvider.get();
		lexer.setCharStream(new ANTLRStringStream(text));
		var tokenSource = new ProblemTokenSource(lexer);
		tokenSource.setIdentifierTokenProvider(identifierTokenProvider);
		return tokenSource;
	}

	private List<Token> createTokenList(String text) {
		var tokenSource = createTokenSource(text);
		var tokens = new ArrayList<Token>();
		Token token;
		do {
			token = tokenSource.nextToken();
			tokens.add(token);
		} while (token.getType() != InternalProblemParser.EOF);
		return tokens;
	}

	private Matcher<Iterable<? super Token>> hasTokenOfType(int tokenId) {
		return hasItem(hasProperty("type", equalTo(tokenId)));
	}
}
