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
		var relativePath = LibraryResolutionUtil.qualifiedNameToPath(qualifiedName);
		if (relativePath == null) {
			return Optional.empty();
		}
		for (var library : libraryPaths) {
			var absoluteResolvedPath = library.resolve(relativePath).toAbsolutePath().normalize();
			if (absoluteResolvedPath.startsWith(library) && Files.exists(absoluteResolvedPath)) {
				var uri = URI.createFileURI(absoluteResolvedPath.toString());
				return Optional.of(uri);
			}
		}
		return Optional.empty();
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
