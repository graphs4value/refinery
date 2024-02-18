/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.library;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.utils.ProblemUtil;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface RefineryLibrary {
	String FILE_NAME_SUFFIX = "." + ProblemUtil.MODULE_EXTENSION;

	default List<QualifiedName> getAutomaticImports() {
		return List.of();
	}

	Optional<URI> resolveQualifiedName(QualifiedName qualifiedName, List<Path> libraryPaths);

	Optional<QualifiedName> getQualifiedName(URI uri, List<Path> libraryPaths);
}
