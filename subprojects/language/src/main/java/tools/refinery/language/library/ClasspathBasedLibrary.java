/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.library;

import com.google.common.collect.Streams;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.naming.QualifiedName;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public abstract class ClasspathBasedLibrary implements RefineryLibrary {
	private final Class<?> context;
	private final List<QualifiedName> mutableSuggestedLibraries = new ArrayList<>();
	private final List<QualifiedName> suggestedLibraries = Collections.unmodifiableList(mutableSuggestedLibraries);
	private final Map<QualifiedName, URI> nameToUriMap = new LinkedHashMap<>();
	private final Map<URI, QualifiedName> uriToNameMap = new LinkedHashMap<>();

	protected ClasspathBasedLibrary(Class<?> context) {
		this.context = context == null ? getClass() : context;

	}

	protected ClasspathBasedLibrary() {
		this(null);
	}

	protected void addLibrary(QualifiedName qualifiedName) {
		var uri = getLibraryUri(context, qualifiedName).orElseThrow(
				() -> new IllegalArgumentException("Failed to resolve library %s in %s"
						.formatted(qualifiedName, context.getName())));
		mutableSuggestedLibraries.add(qualifiedName);
		nameToUriMap.put(qualifiedName, uri);
		uriToNameMap.put(uri, qualifiedName);
    }

	@Override
	public List<QualifiedName> getSuggestedLibraries() {
		return suggestedLibraries;
	}

	@Override
	public Optional<URI> resolveQualifiedName(QualifiedName qualifiedName, List<Path> libraryPaths) {
		return Optional.ofNullable(nameToUriMap.get(qualifiedName));
	}

	@Override
	public Optional<QualifiedName> computeQualifiedName(URI uri, List<Path> libraryPaths) {
		return Optional.ofNullable(uriToNameMap.get(uri));
	}

	public static Optional<URI> getLibraryUri(Class<?> context, QualifiedName qualifiedName) {
		var packagePath = LibraryResolutionUtil.arrayToPath(context.getPackageName().split("\\."));
		if (packagePath == null) {
			throw new IllegalArgumentException("Trying to resolve qualified name in the root package.");
		}
		var relativePath = LibraryResolutionUtil.qualifiedNameToPath(qualifiedName);
		if (relativePath == null) {
			return Optional.empty();
		}
		var path = packagePath.resolve(relativePath).normalize();
		if (!path.startsWith(packagePath)) {
			// Trying to resolve a module outside the library package.
			return Optional.empty();
		}
		var resourceName = Streams.stream(path).map(Path::toString).collect(Collectors.joining("/"));
		var resource = context.getClassLoader().getResource(resourceName);
		if (resource == null) {
			// Library not found.
			return Optional.empty();
		}
		return Optional.of(URI.createURI(resource.toString()));
	}
}
