/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.utils;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.model.problem.*;

public final class ProblemUtil {
	public static final String MODULE_EXTENSION = "refinery";

	private ProblemUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static boolean isBuiltIn(EObject eObject) {
		if (eObject != null) {
			var eResource = eObject.eResource();
			if (eResource != null) {
				return BuiltinLibrary.BUILTIN_LIBRARY_URI.equals(eResource.getURI());
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

	public static boolean isAtomNode(Node node) {
		var containingFeature = node.eContainingFeature();
		if (containingFeature == ProblemPackage.Literals.NODE_DECLARATION__NODES) {
			return ((NodeDeclaration) node.eContainer()).getKind() == NodeKind.ATOM;
		}
		return containingFeature == ProblemPackage.Literals.ENUM_DECLARATION__LITERALS;
	}

	public static boolean isMultiNode(Node node) {
		var containingFeature = node.eContainingFeature();
		if (containingFeature == ProblemPackage.Literals.NODE_DECLARATION__NODES) {
			return ((NodeDeclaration) node.eContainer()).getKind() == NodeKind.MULTI;
		}
		return containingFeature == ProblemPackage.Literals.CLASS_DECLARATION__NEW_NODE;
	}

	public static boolean isDeclaredNode(Node node) {
		return node.eContainingFeature() == ProblemPackage.Literals.NODE_DECLARATION__NODES;
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

	public static ModuleKind getDefaultModuleKind(Problem problem) {
		var resource = problem.eResource();
		if (resource == null) {
			return ModuleKind.PROBLEM;
		}
		return getDefaultModuleKind(resource.getURI());
	}

	public static ModuleKind getDefaultModuleKind(URI uri) {
		return MODULE_EXTENSION.equals(uri.fileExtension()) ? ModuleKind.MODULE : ModuleKind.PROBLEM;
	}

	public static boolean isModule(Problem problem) {
		return problem.getKind() == ModuleKind.MODULE;
	}

	public static boolean isInModule(EObject eObject) {
		var problem = EcoreUtil2.getContainerOfType(eObject, Problem.class);
		return problem != null && isModule(problem);
	}
}
