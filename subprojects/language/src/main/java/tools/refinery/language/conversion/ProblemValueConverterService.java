/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.conversion;

import org.eclipse.xtext.common.services.DefaultTerminalConverters;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;

import com.google.inject.Inject;

public class ProblemValueConverterService extends DefaultTerminalConverters {
	@Inject
	private UpperBoundValueConverter upperBoundValueConverter;

	@ValueConverter(rule = "UpperBound")
	// Method name follows Xtext convention.
	@SuppressWarnings("squid:S100")
	public IValueConverter<Integer> UpperBound() {
		return upperBoundValueConverter;
	}
}
