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

public class BuiltinLibrary implements RefineryLibrary {
	public static final QualifiedName BUILTIN_LIBRARY_NAME = QualifiedName.create("builtin");
	public static final URI BUILTIN_LIBRARY_URI = getLibraryUri(BUILTIN_LIBRARY_NAME).orElseThrow(
			() -> new IllegalStateException("Builtin library was not found"));

	@Override
	public List<QualifiedName> getAutomaticImports() {
		return List.of(BUILTIN_LIBRARY_NAME);
	}

	@Override
	public Optional<URI> resolveQualifiedName(QualifiedName qualifiedName) {
		if (qualifiedName.startsWith(BUILTIN_LIBRARY_NAME)) {
			return getLibraryUri(qualifiedName);
		}
		return Optional.empty();
	}

	private static Optional<URI> getLibraryUri(QualifiedName qualifiedName) {
		var libraryPath = String.join("/", qualifiedName.getSegments());
		var libraryResource = BuiltinLibrary.class.getClassLoader()
				.getResource("tools/refinery/language/library/%s.refinery".formatted(libraryPath));
		if (libraryResource == null) {
			return Optional.empty();
		}
		return Optional.of(URI.createURI(libraryResource.toString()));
	}
}
