/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.conversion;

import com.google.inject.Inject;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.nodemodel.INode;
import tools.refinery.language.naming.NamingUtil;
import tools.refinery.language.parser.antlr.IdentifierTokenProvider;
import tools.refinery.language.services.ProblemGrammarAccess;

import java.util.LinkedHashSet;
import java.util.Set;

public class IdentifierValueConverter implements IValueConverter<String> {
	private final Set<String> keywords;
	private final QUOTED_IDValueConverter quotedIdValueConverter;

	@Inject
	public IdentifierValueConverter(
			ProblemGrammarAccess grammarAccess, QUOTED_IDValueConverter quotedIdValueConverter,
			IdentifierTokenProvider identifierTokenProvider) {
		this.quotedIdValueConverter = quotedIdValueConverter;
		quotedIdValueConverter.setRule(grammarAccess.getQUOTED_IDRule());
		keywords = new LinkedHashSet<>(GrammarUtil.getAllKeywords(grammarAccess.getGrammar()));
		keywords.removeAll(identifierTokenProvider.getIdentifierKeywords());
		// Prevent prototype pollution in JSON output.
		keywords.add("__proto__");
	}

	@Override
	public String toValue(String string, INode node) throws ValueConverterException {
		if (string == null) {
			return null;
		}
		if (NamingUtil.isQuoted(string)) {
			return quotedIdValueConverter.toValue(string, node);
		}
		return string;
	}

	@Override
	public String toString(String value) throws ValueConverterException {
		if (value == null) {
			throw new ValueConverterException("Identifier may not be null.", null, null);
		}
		if (NamingUtil.isSimpleId(value) && !keywords.contains(value)) {
			return value;
		}
		return quotedIdValueConverter.toString(value);
	}
}
