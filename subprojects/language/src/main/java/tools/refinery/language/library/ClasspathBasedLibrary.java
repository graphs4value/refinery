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
	private final QualifiedName prefix;
	private final URI rootUri;

	protected ClasspathBasedLibrary(Class<?> context, QualifiedName prefix) {
		this.context = context == null ? getClass() : context;
		this.prefix = prefix;
		var contextPath = this.context.getCanonicalName().replace('.', '/') + ".class";
		var contextResource = this.context.getClassLoader().getResource(contextPath);
		if (contextResource == null) {
			throw new IllegalStateException("Failed to find library context");
		}
		var contextUri = URI.createURI(contextResource.toString());
		var segments = Arrays.copyOf(contextUri.segments(), contextUri.segmentCount() - 1);
		rootUri = URI.createHierarchicalURI(contextUri.scheme(), contextUri.authority(), contextUri.device(),
				segments, null, null);
	}

	protected ClasspathBasedLibrary(QualifiedName prefix) {
		this(null, prefix);
	}

	@Override
	public Optional<URI> resolveQualifiedName(QualifiedName qualifiedName, List<Path> libraryPaths) {
		if (qualifiedName.startsWith(prefix)) {
			return getLibraryUri(context, qualifiedName);
		}
		return Optional.empty();
	}

	@Override
	public Optional<QualifiedName> getQualifiedName(URI uri, List<Path> libraryPaths) {
		if (!uri.isHierarchical() ||
				!Objects.equals(rootUri.scheme(), uri.scheme()) ||
				!Objects.equals(rootUri.authority(), uri.authority()) ||
				!Objects.equals(rootUri.device(), uri.device()) ||
				rootUri.segmentCount() >= uri.segmentCount()) {
			return Optional.empty();
		}
		int rootSegmentCount = rootUri.segmentCount();
		int uriSegmentCount = uri.segmentCount();
		if (!uri.segment(uriSegmentCount - 1).endsWith(RefineryLibrary.FILE_NAME_SUFFIX)) {
			return Optional.empty();
		}
		var segments = new ArrayList<String>();
		int i = 0;
		while (i < rootSegmentCount) {
			if (!rootUri.segment(i).equals(uri.segment(i))) {
				return Optional.empty();
			}
			i++;
		}
		while (i < uriSegmentCount) {
			var segment = uri.segment(i);
			if (i == uriSegmentCount - 1) {
				segment = segment.substring(0, segment.length() - RefineryLibrary.FILE_NAME_SUFFIX.length());
			}
			segments.add(segment);
			i++;
		}
		var qualifiedName = QualifiedName.create(segments);
		if (!qualifiedName.startsWith(prefix)) {
			return Optional.empty();
		}
		return Optional.of(qualifiedName);
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
