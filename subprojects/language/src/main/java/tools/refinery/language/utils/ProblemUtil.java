/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.utils;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import tools.refinery.language.model.problem.*;

public final class ProblemUtil {
	public static final String BUILTIN_LIBRARY_NAME = "builtin";
	public static final URI BUILTIN_LIBRARY_URI = getLibraryUri();

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

	public static boolean isError(EObject eObject) {
		return eObject instanceof PredicateDefinition predicateDefinition && predicateDefinition.isError();
	}

	public static boolean isIndividualNode(Node node) {
		var containingFeature = node.eContainingFeature();
		return containingFeature == ProblemPackage.Literals.INDIVIDUAL_DECLARATION__NODES
				|| containingFeature == ProblemPackage.Literals.ENUM_DECLARATION__LITERALS;
	}

	public static boolean isNewNode(Node node) {
		return node.eContainingFeature() == ProblemPackage.Literals.CLASS_DECLARATION__NEW_NODE;
	}

	public static boolean isInvalidMultiplicityConstraint(PredicateDefinition predicateDefinition) {
		return predicateDefinition.eContainingFeature() ==
				ProblemPackage.Literals.REFERENCE_DECLARATION__INVALID_MULTIPLICITY;
	}

	public static boolean hasMultiplicityConstraint(ReferenceDeclaration referenceDeclaration) {
		var opposite = referenceDeclaration.getOpposite();
		if (opposite != null && opposite.getKind() == ReferenceKind.CONTAINMENT) {
			return false;
		}
		var multiplicity = referenceDeclaration.getMultiplicity();
		if (multiplicity instanceof UnboundedMultiplicity) {
			return false;
		}
		if (multiplicity instanceof RangeMultiplicity rangeMultiplicity) {
			return rangeMultiplicity.getLowerBound() > 0 || rangeMultiplicity.getUpperBound() >= 0;
		}
		return true;
	}

	public static int getArity(Relation relation) {
		if (relation instanceof ClassDeclaration || relation instanceof EnumDeclaration) {
			return 1;
		}
		if (relation instanceof ReferenceDeclaration) {
			return 2;
		}
		if (relation instanceof PredicateDefinition predicateDefinition) {
			return predicateDefinition.getParameters().size();
		}
		throw new IllegalArgumentException("Unknown Relation: " + relation);
	}

	public static boolean isContainerReference(ReferenceDeclaration referenceDeclaration) {
		var kind = referenceDeclaration.getKind();
		if (kind == null) {
			return false;
		}
		return switch (kind) {
			case CONTAINMENT -> false;
			case CONTAINER -> true;
			case REFERENCE -> {
				var opposite = referenceDeclaration.getOpposite();
				if (opposite == null) {
					yield false;
				}
				opposite = (ReferenceDeclaration) EcoreUtil.resolve(opposite, referenceDeclaration);
				yield opposite.getKind() == ReferenceKind.CONTAINMENT;
			}
		};
	}

	private static URI getLibraryUri() {
		var libraryResource = ProblemUtil.class.getClassLoader()
				.getResource("tools/refinery/language/%s.problem".formatted(BUILTIN_LIBRARY_NAME));
		if (libraryResource == null) {
			throw new AssertionError("Library '%s' was not found".formatted(BUILTIN_LIBRARY_NAME));
		}
		return URI.createURI(libraryResource.toString());
	}
}
