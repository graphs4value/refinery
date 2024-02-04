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
	private final URI rootUri;

	protected ClasspathBasedLibrary(QualifiedName prefix, List<QualifiedName> automaticImports) {
		this.prefix = prefix;
		this.automaticImports = List.copyOf(automaticImports);
		var context = this.getClass();
		var contextPath = context.getCanonicalName().replace('.', '/') + ".class";
		var contextResource = context.getClassLoader().getResource(contextPath);
		if (contextResource == null) {
			throw new IllegalStateException("Failed to find library context");
		}
		var contextUri = URI.createURI(contextResource.toString());
		var segments = Arrays.copyOf(contextUri.segments(), contextUri.segmentCount() - 1);
		rootUri = URI.createHierarchicalURI(contextUri.scheme(), contextUri.authority(), contextUri.device(),
				segments, null, null);
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
		if (!uri.segment(uriSegmentCount - 1).endsWith(RefineryLibrary.EXTENSION)) {
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
				segment = segment.substring(0, segment.length() - RefineryLibrary.EXTENSION.length());
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
