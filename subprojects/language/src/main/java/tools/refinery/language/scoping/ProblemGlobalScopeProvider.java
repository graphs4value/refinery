/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping;

import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.ISelectable;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.AbstractGlobalScopeProvider;
import org.eclipse.xtext.util.IResourceScopeCache;
import tools.refinery.language.resource.LoadOnDemandResourceDescriptionProvider;
import tools.refinery.language.scoping.imports.ImportCollector;
import tools.refinery.language.scoping.imports.NamedImport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ProblemGlobalScopeProvider extends AbstractGlobalScopeProvider {
	private static final String CACHE_KEY = "tools.refinery.language.scoping.ProblemGlobalScopeProvider.CACHE_KEY";

	@Inject
	private ImportCollector importCollector;

	@Inject
	private Provider<LoadOnDemandResourceDescriptionProvider> loadOnDemandProvider;

	@Inject
	private IResourceScopeCache cache;

	// {@link com.google.common.base.Predicate} required by Xtext API.
	@SuppressWarnings("squid:S4738")
	@Override
	protected IScope getScope(Resource resource, boolean ignoreCase, EClass type,
							  Predicate<IEObjectDescription> filter) {
		var loadedImports = cache.get(CACHE_KEY, resource, () -> computeLoadedImports(resource));
		var qualifiedScope = createScope(IScope.NULLSCOPE, loadedImports.qualifiedImports(), type, filter, ignoreCase);
		var implicitScope = createScope(qualifiedScope, loadedImports.implicitImports(), type, filter, ignoreCase);
		return createScope(implicitScope, loadedImports.explicitImports(), type, filter, ignoreCase);
	}

	protected LoadedImports computeLoadedImports(Resource resource) {
		var imports = importCollector.getAllImports(resource);
		var loadOnDemand = loadOnDemandProvider.get();
		loadOnDemand.setContext(resource);
		var qualifiedImports = new ArrayList<ISelectable>();
		var implicitImports = new ArrayList<ISelectable>();
		var explicitImports = new ArrayList<ISelectable>();
		for (var importEntry : imports.toList()) {
			var uri = importEntry.uri();
			var resourceDescription = loadOnDemand.getResourceDescription(uri);
			if (resourceDescription == null) {
				continue;
			}
			qualifiedImports.add(resourceDescription);
			if (importEntry instanceof NamedImport namedImport) {
				var qualifiedName = namedImport.qualifiedName();
				if (namedImport.alsoImplicit()) {
					implicitImports.add(new NormalizedSelectable(resourceDescription, qualifiedName,
							QualifiedName.EMPTY));
				}
				for (var alias : namedImport.aliases()) {
					explicitImports.add(new NormalizedSelectable(resourceDescription, qualifiedName, alias));
				}
			}
		}
		return new LoadedImports(qualifiedImports, implicitImports, explicitImports);
	}

	// {@link com.google.common.base.Predicate} required by Xtext API.
	@SuppressWarnings("squid:S4738")
	protected IScope createScope(IScope parent, Collection<? extends ISelectable> children, EClass type,
								 Predicate<IEObjectDescription> filter, boolean ignoreCase) {
		var selectable = CompositeSelectable.of(children);
		return ShadowingKeyAwareSelectableBasedScope.createScope(parent, selectable, filter, type, ignoreCase);
	}

	protected record LoadedImports(List<ISelectable> qualifiedImports, List<ISelectable> implicitImports,
								   List<ISelectable> explicitImports) {
	}
}
