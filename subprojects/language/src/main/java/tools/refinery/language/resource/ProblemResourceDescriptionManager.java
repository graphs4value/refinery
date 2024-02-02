/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.resource;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.DerivedStateAwareResourceDescriptionManager;
import org.eclipse.xtext.resource.IDefaultResourceDescriptionStrategy;
import org.eclipse.xtext.resource.IResourceDescription;

public class ProblemResourceDescriptionManager extends DerivedStateAwareResourceDescriptionManager {
	@Override
	protected IResourceDescription createResourceDescription(Resource resource,
															 IDefaultResourceDescriptionStrategy strategy) {
		return new ProblemResourceDescription(resource, strategy);
	}
}
