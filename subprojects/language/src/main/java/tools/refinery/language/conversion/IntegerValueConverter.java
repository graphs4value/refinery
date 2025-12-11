/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.conversion;

import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractValueConverter;
import org.eclipse.xtext.nodemodel.INode;
import tools.refinery.logic.term.intinterval.IntBound;

import java.math.BigInteger;

public class IntegerValueConverter extends AbstractValueConverter<BigInteger> {
	@Override
	public BigInteger toValue(String string, INode node) throws ValueConverterException {
		try {
            return new BigInteger(string);
        } catch (NumberFormatException e) {
            throw new ValueConverterException("Invalid integer: " + string, node, e);
        }
	}

	@Override
	public String toString(BigInteger value) throws ValueConverterException {
		if (value == null) {
			throw new ValueConverterException("Integer may not be null.", null, null);
		}
		return IntBound.of(value).toString();
	}
}
