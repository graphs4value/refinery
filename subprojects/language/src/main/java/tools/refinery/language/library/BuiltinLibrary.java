/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.library;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.naming.QualifiedName;

import java.util.List;

public class BuiltinLibrary extends ClasspathBasedLibrary {
	public static final QualifiedName BUILTIN_LIBRARY_NAME = QualifiedName.create("builtin");
	public static final QualifiedName BUILTIN_ANNOTATIONS_LIBRARY_NAME = BUILTIN_LIBRARY_NAME.append("annotations");
	public static final URI BUILTIN_LIBRARY_URI = getBuiltinLibraryUri(BUILTIN_LIBRARY_NAME);
	public static final URI BUILTIN_ANNOTATIONS_LIBRARY_URI = getBuiltinLibraryUri(BUILTIN_ANNOTATIONS_LIBRARY_NAME);

	public BuiltinLibrary() {
		super(BUILTIN_LIBRARY_NAME);
	}

	@Override
	public List<QualifiedName> getAutomaticImports() {
		return List.of(BUILTIN_LIBRARY_NAME);
	}

	private static URI getBuiltinLibraryUri(QualifiedName qualifiedName) {
		return ClasspathBasedLibrary.getLibraryUri(
				BuiltinLibrary.class, qualifiedName).orElseThrow(
				() -> new IllegalStateException("Builtin library %s was not found".formatted(qualifiedName)));
	}
}
