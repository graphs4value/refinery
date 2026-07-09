/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.ibex;

import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.library.ClasspathBasedLibrary;

import java.util.List;

public class IbexLibrary extends ClasspathBasedLibrary {
	public static final QualifiedName IBEX_LIBRARY = QualifiedName.create("builtin", "theory", "ibex");
	public static final QualifiedName IBEX_CORE_LIBRARY = IBEX_LIBRARY.append("core");

	public IbexLibrary() {
		addLibrary(IBEX_LIBRARY);
		addLibrary(IBEX_CORE_LIBRARY);
	}

	@Override
	public List<QualifiedName> getAutomaticImports() {
		return List.of(IBEX_CORE_LIBRARY);
	}
}
