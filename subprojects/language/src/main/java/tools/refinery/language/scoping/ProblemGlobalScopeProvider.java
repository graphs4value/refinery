/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping;

import java.util.LinkedHashSet;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;

import tools.refinery.language.utils.ProblemUtil;

public class ProblemGlobalScopeProvider extends ImportUriGlobalScopeProvider {
	@Override
	protected LinkedHashSet<URI> getImportedUris(Resource resource) {
		LinkedHashSet<URI> importedUris = new LinkedHashSet<>();
		importedUris.add(ProblemUtil.BUILTIN_LIBRARY_URI);
		return importedUris;
	}
}
