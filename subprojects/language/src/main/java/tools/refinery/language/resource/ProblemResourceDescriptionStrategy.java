/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.resource;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.impl.DefaultResourceDescriptionStrategy;
import org.eclipse.xtext.util.IAcceptor;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.naming.NamingUtil;
import tools.refinery.language.utils.ProblemUtil;

import java.util.Map;

@Singleton
public class ProblemResourceDescriptionStrategy extends DefaultResourceDescriptionStrategy {
	public static final String ERROR_PREDICATE = "tools.refinery.language.resource" +
			".ProblemResourceDescriptionStrategy.ERROR_PREDICATE";
	public static final String ERROR_PREDICATE_TRUE = "true";

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Override
	public boolean createEObjectDescriptions(EObject eObject, IAcceptor<IEObjectDescription> acceptor) {
		if (!shouldExport(eObject)) {
			return false;
		}
		var qualifiedName = getNameAsQualifiedName(eObject);
		if (qualifiedName == null) {
			return true;
		}
		var problem = EcoreUtil2.getContainerOfType(eObject, Problem.class);
		var problemQualifiedName = getNameAsQualifiedName(problem);
		var userData = getUserData(eObject);
		boolean nameExported;
		if (shouldExportSimpleName(eObject)) {
			acceptEObjectDescription(eObject, problemQualifiedName, qualifiedName, userData, acceptor);
			nameExported = true;
		} else {
			nameExported = false;
		}
		var parent = eObject.eContainer();
		while (parent != null && parent != problem) {
			var parentQualifiedName = getNameAsQualifiedName(parent);
			if (parentQualifiedName == null) {
				parent = parent.eContainer();
				continue;
			}
			qualifiedName = parentQualifiedName.append(qualifiedName);
			if (shouldExportSimpleName(parent)) {
				acceptEObjectDescription(eObject, problemQualifiedName, qualifiedName, userData, acceptor);
				nameExported = true;
			} else {
				nameExported = false;
			}
			parent = parent.eContainer();
		}
		if (!nameExported) {
			acceptEObjectDescription(eObject, problemQualifiedName, qualifiedName, userData, acceptor);
		}
		return true;
	}

	protected QualifiedName getNameAsQualifiedName(EObject eObject) {
		if (!(eObject instanceof NamedElement namedElement)) {
			return null;
		}
		var name = namedElement.getName();
		if (NamingUtil.isNullOrEmpty(name)) {
			return null;
		}
		return qualifiedNameConverter.toQualifiedName(name);
	}

	protected boolean shouldExport(EObject eObject) {
		if (eObject instanceof Variable) {
			// Variables are always private to the containing predicate definition.
			return false;
		}
		if (eObject instanceof Node node) {
			// Only enum literals and new nodes are visible across problem files.
			return ProblemUtil.isIndividualNode(node) || ProblemUtil.isNewNode(node);
		}
		return true;
	}

	protected Map<String, String> getUserData(EObject eObject) {
		var builder = ImmutableMap.<String, String>builder();
		if (eObject instanceof PredicateDefinition predicateDefinition && predicateDefinition.isError()) {
			builder.put(ERROR_PREDICATE, ERROR_PREDICATE_TRUE);
		}
		return builder.build();
	}

	protected boolean shouldExportSimpleName(EObject eObject) {
		if (eObject instanceof Node node) {
			return !ProblemUtil.isNewNode(node);
		}
		if (eObject instanceof PredicateDefinition predicateDefinition) {
			return !ProblemUtil.isInvalidMultiplicityConstraint(predicateDefinition);
		}
		return true;
	}

	private void acceptEObjectDescription(EObject eObject, QualifiedName prefix, QualifiedName qualifiedName,
										  Map<String, String> userData, IAcceptor<IEObjectDescription> acceptor) {
		var qualifiedNameWithPrefix = prefix == null ? qualifiedName : prefix.append(qualifiedName);
		var description = EObjectDescription.create(qualifiedNameWithPrefix, eObject, userData);
		acceptor.accept(description);
	}
}
