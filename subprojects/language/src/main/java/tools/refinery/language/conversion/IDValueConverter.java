/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.conversion;

import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.conversion.impl.AbstractIDValueConverter;

import java.util.Set;

public class IDValueConverter extends AbstractIDValueConverter {
	@Override
	protected Set<String> computeValuesToEscape(Grammar grammar) {
		return Set.of();
	}

	@Override
	protected boolean mustEscape(String value) {
		// Do not escape keywords with ^, because we use single quotes instead in {@link IdentifierValueConverter}.
		return false;
	}
}
