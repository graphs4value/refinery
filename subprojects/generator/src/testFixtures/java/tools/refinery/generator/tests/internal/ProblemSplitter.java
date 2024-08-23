/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests.internal;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.eclipse.xtext.parser.antlr.Lexer;
import tools.refinery.language.parser.antlr.ProblemTokenSource;
import tools.refinery.language.parser.antlr.lexer.InternalProblemLexer;
import tools.refinery.store.reasoning.literal.Concreteness;

import java.util.regex.Pattern;

@Singleton
public class ProblemSplitter {
	private static final String COMMENT_PREFIX = "(//|%)\\s*";

	private static final Pattern TEST_CASE_PATTERN = Pattern.compile(COMMENT_PREFIX +
			"TEST(?<allowErrors>\\s+WITH\\s+ERRORS)?(\\s*:\\s*(?<name>\\S.*)?)?");

	private static final Pattern EXPECTATION_PATTERN = Pattern.compile(COMMENT_PREFIX +
			"EXPECT(?<candidate>\\s+CANDIDATE)?(?<exact>\\s+EXACTLY)?(\\s*:\\s*(?<description>\\S.*)?)?");

	@Inject
	@Named("org.eclipse.xtext.parser.antlr.Lexer.RUNTIME")
	private Provider<Lexer> lexerProvider;

	@Inject
	private Injector injector;

	public void transformProblem(String problemString, ChunkAcceptor acceptor) {
		var tokenSource = getTokenSource(problemString);
		Token token = tokenSource.nextToken();
		ChunkHeader lastHeader = CommonHeader.INSTANCE;
		int lastStartIndex = 0;
		do {
			if (token.getType() == InternalProblemLexer.RULE_SL_COMMENT) {
				if (!(token instanceof CommonToken commonToken)) {
					throw new IllegalStateException("Unexpected token: " + token);
				}
				var header = parseHeader(token);
				if (header != null) {
					int startIndex = commonToken.getStartIndex();
					var body = problemString.substring(lastStartIndex, startIndex);
					acceptor.acceptChunk(lastHeader, body);
					lastHeader = header;
					lastStartIndex = startIndex;
				}
			}
			token = tokenSource.nextToken();
		} while (token != null && token.getType() != Token.EOF);
		acceptor.acceptChunk(lastHeader, problemString.substring(lastStartIndex));
		acceptor.acceptEnd();
	}

	private TokenSource getTokenSource(String problemString) {
		var charStream = new ANTLRStringStream(problemString);
		var lexer = lexerProvider.get();
		lexer.setCharStream(charStream);
		var tokenSource = new ProblemTokenSource(lexer);
		injector.injectMembers(tokenSource);
		return tokenSource;
	}

	private ChunkHeader parseHeader(Token token) {
		var headerText = token.getText().strip();
		var testCaseMatcher = TEST_CASE_PATTERN.matcher(headerText);
		if (testCaseMatcher.matches()) {
			boolean allowErrors = testCaseMatcher.group("allowErrors") != null;
			return new TestCaseHeader(allowErrors, testCaseMatcher.group("name"));
		}
		var expectationMatcher = EXPECTATION_PATTERN.matcher(headerText);
		if (expectationMatcher.matches()) {
			var concreteness = expectationMatcher.group("candidate") == null ? Concreteness.PARTIAL :
					Concreteness.CANDIDATE;
			var exact = expectationMatcher.group("exact") != null;
			return new ExpectationHeader(concreteness, exact, expectationMatcher.group("description"),
                    token.getLine());
		}
		return null;
	}
}
