/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.library;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.naming.QualifiedName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PathLibrary implements RefineryLibrary {
	@Override
	public Optional<URI> resolveQualifiedName(QualifiedName qualifiedName, List<Path> libraryPaths) {
		if (libraryPaths.isEmpty()) {
			return Optional.empty();
		}
		if (qualifiedName.getSegmentCount() == 0) {
			return Optional.empty();
		}
		var relativePath = qualifiedNameToRelativePath(qualifiedName);
		for (var library : libraryPaths) {
			var absoluteResolvedPath = library.resolve(relativePath).toAbsolutePath().normalize();
			if (absoluteResolvedPath.startsWith(library) && Files.exists(absoluteResolvedPath)) {
				var uri = URI.createFileURI(absoluteResolvedPath.toString());
				return Optional.of(uri);
			}
		}
		return Optional.empty();
	}

	private static Path qualifiedNameToRelativePath(QualifiedName qualifiedName) {
		int segmentCount = qualifiedName.getSegmentCount();
		String first = null;
		var rest = new String[segmentCount - 1];
		for (var i = 0; i < segmentCount; i++) {
			var segment = qualifiedName.getSegment(i);
			if (i == segmentCount - 1) {
				segment = segment + RefineryLibrary.FILE_NAME_SUFFIX;
			}
			if (i == 0) {
				first = segment;
			} else {
				rest[i - 1] = segment;
			}
		}
		if (first == null) {
			throw new AssertionError("Expected qualified name to have non-null segments");
		}
        return Path.of(first, rest);
	}

	@Override
	public Optional<QualifiedName> getQualifiedName(URI uri, List<Path> libraryPaths) {
		if (libraryPaths.isEmpty()) {
			return Optional.empty();
		}
		if (!uri.isFile() || !uri.hasAbsolutePath()) {
			return Optional.empty();
		}
		var path = Path.of(uri.toFileString());
		for (var library : libraryPaths) {
			if (path.startsWith(library)) {
				return getRelativeQualifiedName(library, path);
			}
		}
		return Optional.empty();
	}

	private static Optional<QualifiedName> getRelativeQualifiedName(Path library, Path path) {
		var relativePath = path.relativize(library);
		var segments = new ArrayList<String>();
		for (Path value : relativePath) {
			segments.add(value.toString());
		}
		int lastIndex = segments.size() - 1;
		var lastSegment = segments.get(lastIndex);
		if (!lastSegment.endsWith(FILE_NAME_SUFFIX)) {
			return Optional.empty();
		}
		lastSegment = lastSegment.substring(0, lastSegment.length() - RefineryLibrary.FILE_NAME_SUFFIX.length());
		segments.set(lastIndex, lastSegment);
		return Optional.of(QualifiedName.create(segments));
	}
}
