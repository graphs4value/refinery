/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.library;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.naming.QualifiedName;

import java.util.List;
import java.util.Optional;

public interface RefineryLibrary {
	default List<QualifiedName> getAutomaticImports() {
		return List.of();
	}

	Optional<URI> resolveQualifiedName(QualifiedName qualifiedName);
}
