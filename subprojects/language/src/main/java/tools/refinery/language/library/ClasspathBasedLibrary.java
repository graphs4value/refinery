/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.library;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.naming.QualifiedName;

import java.nio.file.Path;
import java.util.*;

public abstract class ClasspathBasedLibrary implements RefineryLibrary {
	private final QualifiedName prefix;
	private final List<QualifiedName> automaticImports;

	protected ClasspathBasedLibrary(QualifiedName prefix, List<QualifiedName> automaticImports) {
		this.prefix = prefix;
		this.automaticImports = List.copyOf(automaticImports);
	}

	protected ClasspathBasedLibrary(QualifiedName prefix) {
		this(prefix, List.of());
	}

	@Override
	public List<QualifiedName> getAutomaticImports() {
		return automaticImports;
	}

	@Override
	public Optional<URI> resolveQualifiedName(QualifiedName qualifiedName, List<Path> libraryPaths) {
		if (qualifiedName.startsWith(prefix)) {
			return getLibraryUri(this.getClass(), qualifiedName);
		}
		return Optional.empty();
	}

	public static Optional<URI> getLibraryUri(Class<?> context, QualifiedName qualifiedName) {
		var packagePath = context.getPackageName().replace('.', '/');
		var libraryPath = String.join("/", qualifiedName.getSegments());
		var resourceName = "%s/%s%s".formatted(packagePath, libraryPath, RefineryLibrary.EXTENSION);
		var resource = context.getClassLoader().getResource(resourceName);
		if (resource == null) {
			return Optional.empty();
		}
		return Optional.of(URI.createURI(resource.toString()));
	}
}
