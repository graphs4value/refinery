/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.library.internal;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.library.RefineryLibrary;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class CompositeLibrary implements RefineryLibrary {
	private final List<RefineryLibrary> libraries;

	public CompositeLibrary(List<RefineryLibrary> libraries) {
		this.libraries = libraries;
	}

	@Override
	public List<QualifiedName> getAutomaticImports() {
		return libraries.stream()
				.flatMap(library -> library.getAutomaticImports().stream())
                .distinct()
                .toList();
	}

	@Override
	public List<QualifiedName> getSuggestedLibraries() {
		return libraries.stream()
				.flatMap(library -> library.getSuggestedLibraries().stream())
				.distinct()
				.toList();
	}

	@Override
	public Optional<URI> resolveQualifiedName(QualifiedName qualifiedName, List<Path> libraryPaths) {
		for (var library : libraries) {
			var uri = library.resolveQualifiedName(qualifiedName, libraryPaths);
			if (uri.isPresent()) {
				return uri;
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<QualifiedName> computeQualifiedName(URI uri, List<Path> libraryPaths) {
		for (var library : libraries) {
			var qualifiedName = library.computeQualifiedName(uri, libraryPaths);
            if (qualifiedName.isPresent()) {
                return qualifiedName;
            }
		}
		return Optional.empty();
	}
}
