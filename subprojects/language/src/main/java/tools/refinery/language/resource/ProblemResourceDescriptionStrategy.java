/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
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
	private static final String DATA_PREFIX = "tools.refinery.language.resource.ProblemResourceDescriptionStrategy.";
	public static final String ARITY = DATA_PREFIX + "ARITY";
	public static final String ERROR_PREDICATE = DATA_PREFIX + "ERROR_PREDICATE";
	public static final String ERROR_PREDICATE_TRUE = "true";
	public static final String COLOR_RELATION = DATA_PREFIX + "COLOR_RELATION";
	public static final String COLOR_RELATION_TRUE = "true";

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
		QualifiedName lastQualifiedNameToExport = null;
		if (shouldExportSimpleName(eObject)) {
			lastQualifiedNameToExport = qualifiedName;
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
				if (lastQualifiedNameToExport != null) {
					acceptEObjectDescription(eObject, problemQualifiedName, lastQualifiedNameToExport, userData,
							acceptor);
				}
				lastQualifiedNameToExport = qualifiedName;
			}
			parent = parent.eContainer();
		}
		if (lastQualifiedNameToExport == null) {
			lastQualifiedNameToExport = qualifiedName;
		}
		acceptEObjectDescription(eObject, problemQualifiedName, lastQualifiedNameToExport, userData, true, acceptor);
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
			return !ProblemUtil.isImplicitNode(node);
		}
		return true;
	}

	protected Map<String, String> getUserData(EObject eObject) {
		var builder = ImmutableMap.<String, String>builder();
		if (eObject instanceof Relation relation) {
			int arity = ProblemUtil.getArity(relation);
			builder.put(ARITY, Integer.toString(arity));
		}
		if (eObject instanceof PredicateDefinition predicateDefinition && predicateDefinition.isError()) {
			builder.put(ERROR_PREDICATE, ERROR_PREDICATE_TRUE);
		}
		return builder.build();
	}

	protected boolean shouldExportSimpleName(EObject eObject) {
		if (eObject instanceof Node node) {
			return !ProblemUtil.isMultiNode(node);
		}
		if (eObject instanceof PredicateDefinition predicateDefinition) {
			return !ProblemUtil.isInvalidMultiplicityConstraint(predicateDefinition);
		}
		return true;
	}

	private void acceptEObjectDescription(EObject eObject, QualifiedName prefix, QualifiedName qualifiedName,
										  Map<String, String> userData, IAcceptor<IEObjectDescription> acceptor) {
		acceptEObjectDescription(eObject, prefix, qualifiedName, userData, false, acceptor);
	}

	private void acceptEObjectDescription(EObject eObject, QualifiedName prefix, QualifiedName qualifiedName,
										  Map<String, String> userData, boolean fullyQualified,
										  IAcceptor<IEObjectDescription> acceptor) {
		var qualifiedNameWithPrefix = prefix == null ? qualifiedName : prefix.append(qualifiedName);
		Map<String, String> userDataWithFullyQualified;
		if (fullyQualified && shouldColorRelation(eObject)) {
			userDataWithFullyQualified = ImmutableMap.<String, String>builder()
					.putAll(userData)
					.put(COLOR_RELATION, COLOR_RELATION_TRUE)
					.build();
		} else {
			userDataWithFullyQualified = userData;
		}
		var description = EObjectDescription.create(qualifiedNameWithPrefix, eObject, userDataWithFullyQualified);
		acceptor.accept(description);
	}

	private boolean shouldColorRelation(EObject eObject) {
		if (ProblemUtil.isBuiltIn(eObject)) {
			return false;
		}
		return eObject instanceof ClassDeclaration || eObject instanceof EnumDeclaration;
	}
}
