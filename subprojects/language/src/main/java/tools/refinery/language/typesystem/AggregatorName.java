/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.typesystem;

import org.eclipse.xtext.naming.QualifiedName;
import org.jetbrains.annotations.NotNull;

public record AggregatorName(QualifiedName qualifiedName) {
	public AggregatorName(QualifiedName prefix, String name) {
		this(prefix.append(name));
	}

	@Override
	public @NotNull String toString() {
		return qualifiedName.isEmpty() ? "" : qualifiedName.getLastSegment();
	}
}
