/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IResourceDescriptionsProvider;
import org.eclipse.xtext.resource.ISelectable;
import org.eclipse.xtext.scoping.impl.SimpleLocalScopeProvider;
import tools.refinery.language.naming.NamingUtil;

public class ProblemLocalScopeProvider extends SimpleLocalScopeProvider {
	@Inject
	private IQualifiedNameProvider qualifiedNameProvider;

	@Inject
	private IResourceDescriptionsProvider resourceDescriptionsProvider;

	@Override
	protected ISelectable getAllDescriptions(Resource resource) {
		// Force the use of ProblemResourceDescriptionStrategy to include all QualifiedNames of objects.
		var resourceDescriptions = resourceDescriptionsProvider
				.getResourceDescriptions(resource.getResourceSet());
		var resourceDescription = resourceDescriptions.getResourceDescription(resource.getURI());
		if (resourceDescription != null && !resource.getContents().isEmpty()) {
			var rootElement = resource.getContents().getFirst();
			if (rootElement != null) {
				var rootName = NamingUtil.stripRootPrefix(qualifiedNameProvider.getFullyQualifiedName(rootElement));
				if (rootName == null) {
					return resourceDescription;
				}
				return NormalizedSelectable.of(resourceDescription, rootName, QualifiedName.EMPTY);
			}
		}
		return resourceDescription;
	}
}
