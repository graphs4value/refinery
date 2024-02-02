/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.parser.antlr;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.xtext.*;
import org.eclipse.xtext.parser.antlr.ITokenDefProvider;
import tools.refinery.language.services.ProblemGrammarAccess;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class IdentifierTokenProvider {
	private final int[] identifierTokensArray;

	@Inject
	private IdentifierTokenProvider(Initializer initializer) {
		this.identifierTokensArray = initializer.getIdentifierTokesArray();
	}

	public boolean isIdentifierToken(int tokenId) {
		for (int identifierTokenId : identifierTokensArray) {
			if (identifierTokenId == tokenId) {
				return true;
			}
		}
		return false;
	}

	private static class Initializer {
		@Inject
		private ITokenDefProvider tokenDefProvider;

		@Inject
		private ProblemGrammarAccess problemGrammarAccess;

		private HashMap<String, Integer> valueToTokenIdMap;

		private Set<Integer> identifierTokens;

		public int[] getIdentifierTokesArray() {
			createValueToTokenIdMap();
			identifierTokens = new HashSet<>();
			collectIdentifierTokensFromRule(problemGrammarAccess.getIdentifierRule());
			var identifierTokensArray = new int[identifierTokens.size()];
			int i = 0;
			for (var tokenId : identifierTokens) {
				identifierTokensArray[i] = tokenId;
				i++;
			}
			return identifierTokensArray;
		}

		private void createValueToTokenIdMap() {
			var tokenIdToValueMap = tokenDefProvider.getTokenDefMap();
			valueToTokenIdMap = HashMap.newHashMap(tokenIdToValueMap.size());
			for (var entry : tokenIdToValueMap.entrySet()) {
				valueToTokenIdMap.put(entry.getValue(), entry.getKey());
			}
		}

		private void collectIdentifierTokensFromRule(AbstractRule rule) {
			if (rule instanceof TerminalRule) {
				collectToken("RULE_" + rule.getName());
				return;
			}
			collectIdentifierTokensFromElement(rule.getAlternatives());
		}

		private void collectIdentifierTokensFromElement(AbstractElement element) {
            switch (element) {
                case Alternatives alternatives -> {
                    for (var alternative : alternatives.getElements()) {
                        collectIdentifierTokensFromElement(alternative);
                    }
                }
                case RuleCall ruleCall -> collectIdentifierTokensFromRule(ruleCall.getRule());
                case Keyword keyword -> collectToken("'" + keyword.getValue() + "'");
                default -> throw new IllegalArgumentException("Unknown Xtext grammar element: " + element);
            }
		}

		private void collectToken(String value) {
			identifierTokens.add(valueToTokenIdMap.get(value));
		}
	}
}
