/*******************************************************************************
 * Copyright (c) 2008, 2018 itemis AG (http://www.itemis.eu) and others.
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.language.linking;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;

import java.util.*;

public class ProblemLinkingService extends DefaultLinkingService {
	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	private static final Logger logger = Logger.getLogger(ProblemLinkingService.class);

	@Override
	public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {
		final EClass requiredType = ref.getEReferenceType();
		if (requiredType == null) {
			return List.of();
		}
		final String crossRefString = getCrossRefNodeAsString(node);
		if (crossRefString == null || crossRefString.isEmpty()) {
			return List.of();
		}
		if (logger.isDebugEnabled()) {
			logger.debug("before getLinkedObjects: node: '%s'".formatted(crossRefString));
		}
		final IScope scope = getScope(context, ref);
		if (scope == null) {
			throw new AssertionError(("Scope provider must not return null for context %s, reference %s! Consider to" +
					" return IScope.NULLSCOPE instead.").formatted(context, ref));
		}
		final QualifiedName qualifiedLinkName = qualifiedNameConverter.toQualifiedName(crossRefString);
		final Iterator<IEObjectDescription> iterator = scope.getElements(qualifiedLinkName).iterator();
		StringBuilder debug = null;
		final Set<EObject> result = new LinkedHashSet<>();
		if (logger.isDebugEnabled()) {
			debug = new StringBuilder()
					.append("after getLinkedObjects: node: '")
					.append(crossRefString)
					.append("' result: ");
		}
		while (iterator.hasNext()) {
			var eObjectDescription = iterator.next();
			if (debug != null) {
				debug.append(eObjectDescription).append(", ");
			}
			result.add(eObjectDescription.getEObjectOrProxy());
		}
		if (debug != null) {
			logger.debug(debug);
		}
		return List.copyOf(result);
	}
}
