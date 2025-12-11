/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.conversion;

import com.google.inject.Inject;
import org.eclipse.xtext.common.services.DefaultTerminalConverters;
import org.eclipse.xtext.conversion.IValueConverter;
import org.eclipse.xtext.conversion.ValueConverter;

import java.math.BigDecimal;
import java.math.BigInteger;

// Method names in this class follow the Xtext convention and match the corresponding rule name.
@SuppressWarnings("squid:S100")
public class ProblemValueConverterService extends DefaultTerminalConverters {
	@Inject
	private UpperBoundValueConverter upperBoundValueConverter;

	@Inject
	private QUOTED_IDValueConverter quotedIdValueConverter;

	@Inject
	private IdentifierValueConverter identifierValueConverter;

	@Inject
	private IntegerValueConverter integerValueConverter;

	@Inject
	private RealValueConverter realValueConverter;

	@ValueConverter(rule = "UpperBound")
	public IValueConverter<Integer> UpperBound() {
		return upperBoundValueConverter;
	}

	@ValueConverter(rule = "QUOTED_ID")
	public IValueConverter<String> QUOTED_ID() {
		return quotedIdValueConverter;
	}

	@ValueConverter(rule = "Identifier")
	public IValueConverter<String> Identifier() {
		return identifierValueConverter;
	}

	@ValueConverter(rule = "Integer")
	public IValueConverter<BigInteger> Integer() {
		return integerValueConverter;
	}

	@ValueConverter(rule = "Real")
	public IValueConverter<BigDecimal> Real() {
		return realValueConverter;
	}
}
