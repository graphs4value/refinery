/*******************************************************************************
 * Copyright (c) 2010, 2017 itemis AG (http://www.itemis.eu) and others.
 * Copyright (c) 2021-2024 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.language.naming;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.conversion.QUOTED_IDValueConverter;
import tools.refinery.language.services.ProblemGrammarAccess;

import java.util.ArrayList;

/**
 * This class is based on {@link IQualifiedNameConverter.DefaultImpl}, but was extended to handle quoted identifiers.
 *
 * @author Jan Koehnlein - Initial contribution and API
 * @author The Refinery Authors - Modifications for Refinery
 */
@Singleton
public class ProblemQualifiedNameConverter implements IQualifiedNameConverter {
	public static final String DELIMITER = "::";

	private final QUOTED_IDValueConverter valueConverter;

	@Inject
	public ProblemQualifiedNameConverter(ProblemGrammarAccess grammarAccess, QUOTED_IDValueConverter valueConverter) {
		this.valueConverter = valueConverter;
		valueConverter.setRule(grammarAccess.getQUOTED_IDRule());
	}

	/**
	 * Converts the given qualified name to a string.
	 *
	 * @throws IllegalArgumentException when the qualified name is null.
	 */
	@Override
	public String toString(QualifiedName qualifiedName) {
		if (qualifiedName == null) {
			throw new IllegalArgumentException("Qualified name cannot be null");
		}
		var builder = new StringBuilder();
		int segmentCount = qualifiedName.getSegmentCount();
		for (int i = 0; i < segmentCount; i++) {
			if (i > 0) {
				builder.append(DELIMITER);
			}
			var segment = qualifiedName.getSegment(i);
			if (segment.isEmpty() || NamingUtil.isSimpleId(segment)) {
				builder.append(segment);
			} else {
				builder.append(valueConverter.toString(segment));
			}
		}
		return builder.toString();
	}

	/**
	 * Splits the given string into segments and returns them as a {@link QualifiedName}.
	 *
	 * @throws IllegalArgumentException if the input is empty or null.
	 */
	@Override
	public QualifiedName toQualifiedName(String qualifiedNameAsString) {
		Preconditions.checkArgument(qualifiedNameAsString != null, "Qualified name cannot be null");
		Preconditions.checkArgument(!qualifiedNameAsString.isEmpty(), "Qualified name cannot be empty");
		var segments = new ArrayList<String>();
		int length = qualifiedNameAsString.length();
		int delimiterLength = DELIMITER.length();
		int index = 0;
		if (qualifiedNameAsString.startsWith(DELIMITER)) {
			// Absolute name.
			segments.add("");
			index = delimiterLength;
		}
		while (index < length) {
			int endIndex = NamingUtil.scanName(qualifiedNameAsString, index);
			if (endIndex <= 0) {
				throw new IllegalArgumentException("Invalid qualified name: " + qualifiedNameAsString);
			}
			String segment = qualifiedNameAsString.substring(index, endIndex);
			if (NamingUtil.isQuoted(segment)) {
				segment = valueConverter.toValue(segment, null);
			}
			segments.add(segment);
			index = endIndex + delimiterLength;
		}
		return QualifiedName.create(segments);
	}
}
