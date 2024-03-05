/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.typesystem;

import org.eclipse.xtext.naming.QualifiedName;

public record DataExprType(QualifiedName qualifiedName) implements FixedType {
	public DataExprType(QualifiedName prefix, String name) {
		this(prefix.append(name));
	}

	@Override
	public String toString() {
		return qualifiedName.isEmpty() ? "" : qualifiedName.getLastSegment();
	}
}
