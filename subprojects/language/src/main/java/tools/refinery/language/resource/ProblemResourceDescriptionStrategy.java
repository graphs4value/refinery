/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.resource;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.impl.DefaultResourceDescriptionStrategy;
import org.eclipse.xtext.util.IAcceptor;
import tools.refinery.language.naming.ProblemQualifiedNameProvider;
import tools.refinery.language.scoping.imports.ImportCollector;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.naming.NamingUtil;
import tools.refinery.language.utils.ProblemUtil;

import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class ProblemResourceDescriptionStrategy extends DefaultResourceDescriptionStrategy {
	private static final String DATA_PREFIX = "tools.refinery.language.resource.ProblemResourceDescriptionStrategy.";

	public static final String ARITY = DATA_PREFIX + "ARITY";
	public static final String ERROR_PREDICATE = DATA_PREFIX + "ERROR_PREDICATE";
	public static final String ERROR_PREDICATE_TRUE = "true";
	public static final String SHADOWING_KEY = DATA_PREFIX + "SHADOWING_KEY";
	public static final String SHADOWING_KEY_PROBLEM = "problem";
	public static final String SHADOWING_KEY_NODE = "node";
	public static final String SHADOWING_KEY_RELATION = "relation";
	public static final String PREFERRED_NAME = DATA_PREFIX + "PREFERRED_NAME";
	public static final String PREFERRED_NAME_TRUE = "true";
	public static final String IMPORTS = DATA_PREFIX + "IMPORTS";
	public static final String IMPORTS_SEPARATOR = "|";
	public static final String MODULE_KIND = DATA_PREFIX + "MODULE_KIND";
	public static final String COLOR_RELATION = DATA_PREFIX + "COLOR_RELATION";
	public static final String COLOR_RELATION_TRUE = "true";

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	@Named(ProblemQualifiedNameProvider.NAMED_DELEGATE)
	private IQualifiedNameProvider delegateQualifiedNameProvider;

	@Inject
	private ImportCollector importCollector;

	@Override
	public boolean createEObjectDescriptions(EObject eObject, IAcceptor<IEObjectDescription> acceptor) {
		if (!shouldExport(eObject)) {
			return false;
		}
		var problem = EcoreUtil2.getContainerOfType(eObject, Problem.class);
		var problemQualifiedName = getProblemQualifiedName(problem);
		var userData = getUserData(eObject);
		if (eObject.equals(problem)) {
			acceptEObjectDescription(eObject, problemQualifiedName, QualifiedName.EMPTY, userData, true, acceptor);
			return true;
		}
		var qualifiedName = getNameAsQualifiedName(eObject);
		if (qualifiedName == null) {
			return true;
		}
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
		var qualifiedName = qualifiedNameConverter.toQualifiedName(name);
		if (eObject instanceof Problem) {
			return NamingUtil.stripRootPrefix(qualifiedName);
		}
		return qualifiedName;
	}

	protected QualifiedName getProblemQualifiedName(Problem problem) {
		if (problem == null) {
			return QualifiedName.EMPTY;
		}
		var qualifiedName = delegateQualifiedNameProvider.getFullyQualifiedName(problem);
        return qualifiedName == null ? QualifiedName.EMPTY : qualifiedName;
    }

	public static boolean shouldExport(EObject eObject) {
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
		if (eObject instanceof Problem problem) {
			builder.put(SHADOWING_KEY, SHADOWING_KEY_PROBLEM);
			var explicitImports = importCollector.getDirectImports(eObject.eResource());
			var importsString = explicitImports.toList().stream()
					.map(importEntry -> importEntry.uri().toString())
					.collect(Collectors.joining(IMPORTS_SEPARATOR));
			builder.put(IMPORTS, importsString);
			builder.put(MODULE_KIND, problem.getKind().getName());
		} else if (eObject instanceof Node) {
			builder.put(SHADOWING_KEY, SHADOWING_KEY_NODE);
		} else if (eObject instanceof Relation relation) {
			builder.put(SHADOWING_KEY, SHADOWING_KEY_RELATION);
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
										  Map<String, String> userData, boolean preferredName,
										  IAcceptor<IEObjectDescription> acceptor) {
		var qualifiedNameWithPrefix = prefix == null ? qualifiedName : prefix.append(qualifiedName);
		var userDataWithPreference = userData;
		if (preferredName) {
			userDataWithPreference = ImmutableMap.<String, String>builder()
					.putAll(userData)
					.put(PREFERRED_NAME, PREFERRED_NAME_TRUE)
					.build();
		}
		var description = EObjectDescription.create(qualifiedNameWithPrefix, eObject, userDataWithPreference);
		acceptor.accept(description);
		if (!preferredName) {
			return;
		}
		var userDataWithFullyQualified = userDataWithPreference;
		if (shouldColorRelation(eObject)) {
			userDataWithFullyQualified = ImmutableMap.<String, String>builder()
					.putAll(userDataWithPreference)
					.put(COLOR_RELATION, COLOR_RELATION_TRUE)
					.build();
		}
		var rootQualifiedName = NamingUtil.addRootPrefix(qualifiedNameWithPrefix);
		var rootDescription = EObjectDescription.create(rootQualifiedName, eObject, userDataWithFullyQualified);
		acceptor.accept(rootDescription);
	}

	private boolean shouldColorRelation(EObject eObject) {
		if (ProblemUtil.isBuiltIn(eObject)) {
			return false;
		}
		return eObject instanceof ClassDeclaration || eObject instanceof EnumDeclaration;
	}

	public static ShadowingKey getShadowingKey(IEObjectDescription description) {
		return new ShadowingKey(description.getName(), description.getUserData(SHADOWING_KEY));
	}

	public record ShadowingKey(QualifiedName name, String shadowingKey) {
	}
}
