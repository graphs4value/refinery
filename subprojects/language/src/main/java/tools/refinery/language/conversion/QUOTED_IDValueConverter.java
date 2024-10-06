/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.conversion;

import org.eclipse.xtext.conversion.impl.STRINGValueConverter;
import org.eclipse.xtext.util.Strings;

// Class name follows Xtext conventions by including the grammar rule name.
@SuppressWarnings("squid:S101")
public class QUOTED_IDValueConverter extends STRINGValueConverter {
	@Override
	protected String toEscapedString(String value) {
		return '\'' + Strings.convertToJavaString(value, false) + '\'';
	}
}
