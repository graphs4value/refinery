/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.resource;

import com.google.inject.Inject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.IResourceDescriptionsProvider;
import org.eclipse.xtext.scoping.impl.GlobalResourceDescriptionProvider;

public class LoadOnDemandResourceDescriptionProvider {
	@Inject
	private IResourceDescriptionsProvider resourceDescriptionsProvider;

	@Inject
	private GlobalResourceDescriptionProvider globalResourceDescriptionProvider;

	private Resource context;
	private IResourceDescriptions resourceDescriptions;

	public void setContext(Resource context) {
		if (this.context != null) {
			throw new IllegalStateException("Context was already set");
		}
		this.context = context;
		resourceDescriptions = resourceDescriptionsProvider.getResourceDescriptions(context.getResourceSet());
	}

	public IResourceDescription getResourceDescription(URI uri) {
		if (this.context == null) {
			throw new IllegalStateException("Context was not set");
		}
		var resourceDescription = resourceDescriptions.getResourceDescription(uri);
		if (resourceDescription != null) {
			return resourceDescription;
		}
		var importedResource = EcoreUtil2.getResource(context, uri.toString());
		if (importedResource == null) {
			return null;
		}
		// Force the {@code importedResource} to have all of its derived resource state installed.
		EcoreUtil.resolveAll(importedResource);
		return globalResourceDescriptionProvider.getResourceDescription(importedResource);
	}
}
