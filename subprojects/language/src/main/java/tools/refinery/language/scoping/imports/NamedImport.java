/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping.imports;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.naming.QualifiedName;

import java.util.List;

public record NamedImport(URI uri, QualifiedName qualifiedName, List<QualifiedName> aliases,
						  boolean alsoImplicit) implements Import {
	public static NamedImport implicit(URI uri, QualifiedName qualifiedName) {
		return new NamedImport(uri, qualifiedName, List.of(), true);
	}

	public static NamedImport explicit(URI uri, QualifiedName qualifiedName, List<QualifiedName> aliases) {
		return new NamedImport(uri, qualifiedName, aliases, false);
	}
}
