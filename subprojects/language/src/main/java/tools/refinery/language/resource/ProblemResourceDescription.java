/*******************************************************************************
 * Copyright (c) 2009, 2011 itemis AG (http://www.itemis.eu) and others.
 * Copyright (c) 2024 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.language.resource;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.IDefaultResourceDescriptionStrategy;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.impl.DefaultResourceDescription;
import org.eclipse.xtext.resource.impl.EObjectDescriptionLookUp;
import org.eclipse.xtext.util.IAcceptor;
import tools.refinery.language.naming.NamingUtil;

import java.io.IOException;
import java.util.*;

/**
 * A resource description that takes {@link ProblemResourceDescriptionStrategy#SHADOWING_KEY} into account when
 * describing EObjects.
 * <p>
 * Based on {@link DefaultResourceDescription}.
 */
public class ProblemResourceDescription extends DefaultResourceDescription {
	private static final Logger log = Logger.getLogger(ProblemResourceDescription.class);

	private final IDefaultResourceDescriptionStrategy strategy;

	public ProblemResourceDescription(Resource resource, IDefaultResourceDescriptionStrategy strategy) {
		super(resource, strategy);
		this.strategy = strategy;
	}

	/**
	 * Based on {@link DefaultResourceDescription#computeExportedObjects()}.
	 *
	 * @return The computed exported objects, taking shadowing into account.
	 */
	@Override
	protected List<IEObjectDescription> computeExportedObjects() {
		if (!getResource().isLoaded()) {
			try {
				getResource().load(null);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
				return Collections.emptyList();
			}
		}
		final Map<ProblemResourceDescriptionStrategy.ShadowingKey, List<IEObjectDescription>> nameToDescriptionsMap =
				new LinkedHashMap<>();
		IAcceptor<IEObjectDescription> acceptor = eObjectDescription -> {
			var key = ProblemResourceDescriptionStrategy.getShadowingKey(eObjectDescription);
			var descriptions = nameToDescriptionsMap.computeIfAbsent(key, ignored -> new ArrayList<>());
			descriptions.add(eObjectDescription);
		};
		TreeIterator<EObject> allProperContents = EcoreUtil.getAllProperContents(getResource(), false);
		while (allProperContents.hasNext()) {
			EObject content = allProperContents.next();
			if (!strategy.createEObjectDescriptions(content, acceptor)) {
				allProperContents.prune();
			}
		}
		return omitShadowedNames(nameToDescriptionsMap);
	}

	private static List<IEObjectDescription> omitShadowedNames(
			Map<ProblemResourceDescriptionStrategy.ShadowingKey, List<IEObjectDescription>> nameToDescriptionsMap) {
		final List<IEObjectDescription> exportedEObjects = new ArrayList<>();
		for (var entry : nameToDescriptionsMap.entrySet()) {
			var descriptions = entry.getValue();
			if (NamingUtil.isFullyQualified(entry.getKey().name())) {
				exportedEObjects.addAll(descriptions);
			} else {
				boolean foundPreferred = false;
				for (var description : descriptions) {
					if (ProblemResourceDescriptionStrategy.PREFERRED_NAME_TRUE.equals(
							description.getUserData(ProblemResourceDescriptionStrategy.PREFERRED_NAME))) {
						exportedEObjects.add(description);
						foundPreferred = true;
					}
				}
				if (!foundPreferred) {
					exportedEObjects.addAll(descriptions);
				}
			}
		}
		return exportedEObjects;
	}

	// Based on {@code DerivedStateAwareResourceDescriptionManager#createResourceDescription}.
	@Override
	protected EObjectDescriptionLookUp getLookUp() {
		if (lookup == null) {
			lookup = new EObjectDescriptionLookUp(computeExportedObjects());
		}
		return lookup;
	}
}
