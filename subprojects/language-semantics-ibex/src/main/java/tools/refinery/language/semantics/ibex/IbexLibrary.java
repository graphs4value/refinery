/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.ibex;

import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.library.ClasspathBasedLibrary;

public class IbexLibrary extends ClasspathBasedLibrary {
	public static final QualifiedName IBEX_LIBRARY = QualifiedName.create("builtin", "theory", "ibex");

	public IbexLibrary() {
		addLibrary(IBEX_LIBRARY);
	}
}
