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
import java.util.LinkedHashSet;
import java.util.Set;

@Singleton
public class IdentifierTokenProvider {
	private final int[] identifierTokensArray;
	private final Set<String> identifierKeywords;

	@Inject
	private IdentifierTokenProvider(Initializer initializer) {
		identifierTokensArray = initializer.getIdentifierTokesArray();
		identifierKeywords = initializer.getIdentifierKeywords();
	}

	public boolean isIdentifierToken(int tokenId) {
		for (int identifierTokenId : identifierTokensArray) {
			if (identifierTokenId == tokenId) {
				return true;
			}
		}
		return false;
	}

	public Set<String> getIdentifierKeywords() {
		return identifierKeywords;
	}

	private static class Initializer {
		@Inject
		private ITokenDefProvider tokenDefProvider;

		@Inject
		private ProblemGrammarAccess problemGrammarAccess;

		private HashMap<String, Integer> valueToTokenIdMap;
		private Set<Integer> identifierTokens;
		private Set<String> identifierKeywords;

		public int[] getIdentifierTokesArray() {
			createValueToTokenIdMap();
			identifierTokens = new LinkedHashSet<>();
			identifierKeywords = new LinkedHashSet<>();
			collectIdentifierTokensFromRule(problemGrammarAccess.getIdentifierRule());
			var identifierTokensArray = new int[identifierTokens.size()];
			int i = 0;
			for (var tokenId : identifierTokens) {
				identifierTokensArray[i] = tokenId;
				i++;
			}
			return identifierTokensArray;
		}

		public Set<String> getIdentifierKeywords() {
			return Set.copyOf(identifierKeywords);
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
                case Keyword keyword -> {
					var value = keyword.getValue();
					collectToken("'" + value + "'");
					identifierKeywords.add(value);
				}
                default -> throw new IllegalArgumentException("Unknown Xtext grammar element: " + element);
            }
		}

		private void collectToken(String value) {
			identifierTokens.add(valueToTokenIdMap.get(value));
		}
	}
}
