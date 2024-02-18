/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.naming;

import org.eclipse.xtext.naming.IQualifiedNameConverter;

import com.google.inject.Singleton;

@Singleton
public class ProblemQualifiedNameConverter extends IQualifiedNameConverter.DefaultImpl {
	public static final String DELIMITER = "::";

	@Override
	public String getDelimiter() {
		return DELIMITER;
	}
}
