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
	public static final URI BUILTIN_LIBRARY_URI = ClasspathBasedLibrary.getLibraryUri(
			BuiltinLibrary.class, BUILTIN_LIBRARY_NAME).orElseThrow(
			() -> new IllegalStateException("Builtin library was not found"));

	public BuiltinLibrary() {
		super(BUILTIN_LIBRARY_NAME);
	}

	@Override
	public List<QualifiedName> getAutomaticImports() {
		return List.of(BUILTIN_LIBRARY_NAME);
	}
}
