package tools.refinery.language.parser.antlr;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.TokenSource;

public class TokenSourceInjectingProblemParser extends ProblemParser {
	@Inject
	private Injector injector;

	@Override
	protected TokenSource createLexer(CharStream stream) {
		var tokenSource = super.createLexer(stream);
		injector.injectMembers(tokenSource);
		return tokenSource;
	}
}
