/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * Copyright (c) 2024 The Refinery Authors <https://refinery.tools/>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.language.naming;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.DefaultDeclarativeQualifiedNameProvider;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.util.Strings;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;
import tools.refinery.language.utils.ProblemUtil;

@Singleton
public class ProblemQualifiedNameProvider extends DefaultDeclarativeQualifiedNameProvider {
	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private ImportAdapterProvider importAdapterProvider;

	protected QualifiedName qualifiedName(Problem problem) {
		var qualifiedNameString = problem.getName();
		if (qualifiedNameString != null) {
			return NamingUtil.stripRootPrefix(qualifiedNameConverter.toQualifiedName(qualifiedNameString));
		}
		if (!ProblemUtil.isModule(problem)) {
			return null;
		}
		var resource = problem.eResource();
		if (resource == null) {
			return null;
		}
		var resourceUri = resource.getURI();
		if (resourceUri == null) {
			return null;
		}
		var resourceSet = resource.getResourceSet();
		if (resourceSet == null) {
			return null;
		}
		var adapter = importAdapterProvider.getOrInstall(resourceSet);
		// If a module has no explicitly specified name, return the qualified name it was resolved under.
		return adapter.getQualifiedName(resourceUri);
	}

	/**
	 * A modified version of
	 * {@link DefaultDeclarativeQualifiedNameProvider#computeFullyQualifiedNameFromNameAttribute(EObject)}
	 * that constructs qualified name fragments without invoking {@link IQualifiedNameConverter} to avoid applying
	 * qualified name escaping rules twice.
	 *
	 * @param obj The object to compute the fully qualified name of.
	 * @return The fully qualified name.
	 */
	@Override
	protected QualifiedName computeFullyQualifiedNameFromNameAttribute(EObject obj) {
		String name = getResolver().apply(obj);
		if (Strings.isEmpty(name)) {
			return null;
		}
		QualifiedName qualifiedNameFromConverter = QualifiedName.create(name);
		while (obj.eContainer() != null) {
			obj = obj.eContainer();
			QualifiedName parentsQualifiedName = getFullyQualifiedName(obj);
			if (parentsQualifiedName != null)
				return parentsQualifiedName.append(qualifiedNameFromConverter);
		}
		return qualifiedNameFromConverter;
	}
}
