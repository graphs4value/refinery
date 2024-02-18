/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IResourceDescriptionsProvider;
import org.eclipse.xtext.resource.ISelectable;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.AbstractGlobalScopeDelegatingScopeProvider;
import org.eclipse.xtext.util.IResourceScopeCache;
import tools.refinery.language.naming.ProblemQualifiedNameProvider;

public class ProblemLocalScopeProvider extends AbstractGlobalScopeDelegatingScopeProvider {
	private static final String CACHE_KEY = "tools.refinery.language.scoping.ProblemLocalScopeProvider.CACHE_KEY";

	@Inject
	@Named(ProblemQualifiedNameProvider.NAMED_DELEGATE)
	private IQualifiedNameProvider delegateQualifiedNameProvider;

	@Inject
	private IResourceDescriptionsProvider resourceDescriptionsProvider;

	@Inject
	private IResourceScopeCache cache;

	@Override
	public IScope getScope(EObject context, EReference reference) {
		var resource = context.eResource();
		if (resource == null) {
			return IScope.NULLSCOPE;
		}
		var globalScope = getGlobalScope(resource, reference);
		var localImports = cache.get(CACHE_KEY, resource, () -> computeLocalImports(resource));
		if (localImports == null) {
			return globalScope;
		}
		var type = reference.getEReferenceType();
		boolean ignoreCase = isIgnoreCase(reference);
		return ShadowingKeyAwareSelectableBasedScope.createScope(globalScope, localImports, type, ignoreCase);
	}

	protected ISelectable computeLocalImports(Resource resource) {
		// Force the use of ProblemResourceDescriptionStrategy to include all QualifiedNames of objects.
		var resourceDescriptions = resourceDescriptionsProvider.getResourceDescriptions(resource.getResourceSet());
		var resourceDescription = resourceDescriptions.getResourceDescription(resource.getURI());
		if (resourceDescription == null) {
			return null;
		}
		var rootElement = resource.getContents().getFirst();
		if (rootElement == null) {
			return new NoFullyQualifiedNamesSelectable(resourceDescription);
		}
		var rootName = delegateQualifiedNameProvider.getFullyQualifiedName(rootElement);
		if (rootName == null) {
			return new NoFullyQualifiedNamesSelectable(resourceDescription);
		}
		return new NormalizedSelectable(resourceDescription, rootName, QualifiedName.EMPTY);
	}
}
