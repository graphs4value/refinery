/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping;

import com.google.common.base.Predicate;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.ISelectable;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;
import org.eclipse.xtext.scoping.impl.SelectableBasedScope;
import tools.refinery.language.utils.ProblemUtil;

import java.util.LinkedHashSet;

public class ProblemGlobalScopeProvider extends ImportUriGlobalScopeProvider {
	@Override
	protected LinkedHashSet<URI> getImportedUris(Resource resource) {
		LinkedHashSet<URI> importedUris = new LinkedHashSet<>();
		importedUris.add(ProblemUtil.BUILTIN_LIBRARY_URI);
		return importedUris;
	}

	@Override
	protected IScope createLazyResourceScope(IScope parent, URI uri, IResourceDescriptions descriptions, EClass type,
											 Predicate<IEObjectDescription> filter, boolean ignoreCase) {
		ISelectable description = descriptions.getResourceDescription(uri);
		if (description != null && ProblemUtil.BUILTIN_LIBRARY_URI.equals(uri)) {
			description = NormalizedSelectable.of(description, QualifiedName.create("builtin"), QualifiedName.EMPTY);
		}
		return SelectableBasedScope.createScope(parent, description, filter, type, ignoreCase);
	}
}
