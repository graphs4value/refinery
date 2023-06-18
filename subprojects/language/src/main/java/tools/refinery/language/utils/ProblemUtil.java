/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.utils;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;

import tools.refinery.language.model.problem.ImplicitVariable;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.ProblemPackage;
import tools.refinery.language.model.problem.Variable;

public final class ProblemUtil {
	public static final String BUILTIN_LIBRARY_NAME = "builtin";

	public static final URI BUILTIN_LIBRARY_URI = getLibraryUri(BUILTIN_LIBRARY_NAME);

	public static final String NODE_CLASS_NAME = "node";

	public static final String DOMAIN_CLASS_NAME = "domain";

	public static final String DATA_CLASS_NAME = "data";

	public static final String INT_CLASS_NAME = "int";

	public static final String REAL_CLASS_NAME = "real";

	public static final String STRING_CLASS_NAME = "string";

	public static final String EQUALS_RELATION_NAME = "equals";

	public static final String EXISTS_PREDICATE_NAME = "exists";

	private ProblemUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static boolean isBuiltIn(EObject eObject) {
		if (eObject != null) {
			var eResource = eObject.eResource();
			if (eResource != null) {
				return ProblemUtil.BUILTIN_LIBRARY_URI.equals(eResource.getURI());
			}
		}
		return false;
	}

	public static boolean isSingletonVariable(Variable variable) {
		return variable.eContainingFeature() == ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__SINGLETON_VARIABLE;
	}

	public static boolean isImplicitVariable(Variable variable) {
		return variable instanceof ImplicitVariable;
	}

	public static boolean isImplicitNode(Node node) {
		return node.eContainingFeature() == ProblemPackage.Literals.PROBLEM__NODES;
	}

	public static boolean isImplicit(EObject eObject) {
		if (eObject instanceof Node node) {
			return isImplicitNode(node);
		} else if (eObject instanceof Variable variable) {
			return isImplicitVariable(variable);
		} else {
			return false;
		}
	}

	public static boolean isIndividualNode(Node node) {
		var containingFeature = node.eContainingFeature();
		return containingFeature == ProblemPackage.Literals.INDIVIDUAL_DECLARATION__NODES
				|| containingFeature == ProblemPackage.Literals.ENUM_DECLARATION__LITERALS;
	}

	public static boolean isNewNode(Node node) {
		return node.eContainingFeature() == ProblemPackage.Literals.CLASS_DECLARATION__NEW_NODE;
	}

	private static URI getLibraryUri(String libraryName) {
		return URI.createURI(ProblemUtil.class.getClassLoader()
				.getResource("tools/refinery/language/%s.problem".formatted(libraryName)).toString());
	}
}
