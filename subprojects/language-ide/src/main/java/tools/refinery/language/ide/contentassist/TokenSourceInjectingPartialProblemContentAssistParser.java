/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.ide.contentassist;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.TokenSource;
import tools.refinery.language.ide.contentassist.antlr.PartialProblemContentAssistParser;

/**
 * Code is duplicated from {@link tools.refinery.language.parser.antlr.TokenSourceInjectingProblemParser} due to
 * Xtext code generation.
 */
public class TokenSourceInjectingPartialProblemContentAssistParser extends PartialProblemContentAssistParser {
	@Inject
	private Injector injector;

	@Override
	protected TokenSource createLexer(CharStream stream) {
		var tokenSource = super.createLexer(stream);
		injector.injectMembers(tokenSource);
		return tokenSource;
	}
}
