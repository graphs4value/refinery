/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.library;

import org.eclipse.xtext.naming.QualifiedName;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

public final class LibraryResolutionUtil {
	private LibraryResolutionUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	@Nullable
	public static Path arrayToPath(String[] array) {
		if (array.length == 0) {
			return null;
		}
		return Path.of(array[0], Arrays.stream(array).skip(1).toArray(String[]::new));
	}

	@Nullable
	public static Path qualifiedNameToPath(QualifiedName qualifiedName) {
		if (!isValidPathName(qualifiedName)) {
			return null;
		}
		var pathSegments = qualifiedName.getSegments().toArray(new String[0]);
		int lastSegmentIndex = qualifiedName.getSegmentCount() - 1;
		if (lastSegmentIndex < 0) {
			// Trying to resolve empty qualified name.
			return null;
		}
		pathSegments[lastSegmentIndex] += RefineryLibrary.FILE_NAME_SUFFIX;
		return arrayToPath(pathSegments);
	}

	private static boolean isValidPathName(QualifiedName qualifiedName) {
		for (var segment : qualifiedName.getSegments()) {
			if (segment.contains("/") ||
					segment.contains(File.separator) ||
					".".equals(segment) ||
					"..".equals(segment)) {
				// Invalid character in qualified name.
				return false;
			}
		}
		return true;
	}
}
