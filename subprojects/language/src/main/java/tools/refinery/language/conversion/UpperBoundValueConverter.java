/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.conversion;

import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractValueConverter;
import org.eclipse.xtext.conversion.impl.INTValueConverter;
import org.eclipse.xtext.nodemodel.INode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UpperBoundValueConverter extends AbstractValueConverter<Integer> {
	public static final String INFINITY = "*";	
	
	@Inject
	private INTValueConverter intValueConverter;
	
	@Override
	public Integer toValue(String string, INode node) throws ValueConverterException {
		if (INFINITY.equals(string)) {
			return -1;
		} else {
			return intValueConverter.toValue(string, node);
		}
	}

	@Override
	public String toString(Integer value) throws ValueConverterException {
		if (value < 0) {
			return INFINITY;
		} else {
			return intValueConverter.toString(value);
		}
	}
}
