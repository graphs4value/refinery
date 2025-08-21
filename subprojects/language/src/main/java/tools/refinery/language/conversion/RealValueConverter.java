/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.conversion;

import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractValueConverter;
import org.eclipse.xtext.nodemodel.INode;
import tools.refinery.logic.term.realinterval.RealBound;

import java.math.BigDecimal;

public class RealValueConverter extends AbstractValueConverter<BigDecimal> {
	@Override
	public BigDecimal toValue(String string, INode node) throws ValueConverterException {
		try {
            return new BigDecimal(string);
        } catch (NumberFormatException e) {
            throw new ValueConverterException("Invalid real number: " + string, node, e);
        }
	}

	@Override
	public String toString(BigDecimal value) throws ValueConverterException {
		if (value == null) {
			throw new ValueConverterException("Real number may not be null.", null, null);
		}
		return RealBound.of(value).toString();
	}
}
