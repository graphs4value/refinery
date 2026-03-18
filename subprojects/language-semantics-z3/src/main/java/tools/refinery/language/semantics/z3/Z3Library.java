/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.z3;

import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.library.ClasspathBasedLibrary;

import java.util.List;

public class Z3Library extends ClasspathBasedLibrary {
	public static final QualifiedName Z3_LIBRARY = QualifiedName.create("builtin", "theory", "z3");
	public static final QualifiedName Z3_CORE_LIBRARY = Z3_LIBRARY.append("core");

	public Z3Library() {
		addLibrary(Z3_LIBRARY);
		addLibrary(Z3_CORE_LIBRARY);
	}

	@Override
	public List<QualifiedName> getAutomaticImports() {
		return List.of(Z3_CORE_LIBRARY);
	}
}
