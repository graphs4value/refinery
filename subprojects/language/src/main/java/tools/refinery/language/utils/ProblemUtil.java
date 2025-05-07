/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
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
		return switch (eObject) {
			case Node node -> isImplicitNode(node);
			case Variable variable -> isImplicitVariable(variable);
			default -> false;
		};
	}

	public static boolean isError(EObject eObject) {
		return eObject instanceof PredicateDefinition predicateDefinition &&
				predicateDefinition.getKind() == PredicateKind.ERROR;
	}

	public static boolean isShadow(EObject eObject) {
		return eObject instanceof PredicateDefinition predicateDefinition &&
				predicateDefinition.getKind() == PredicateKind.SHADOW;
	}

	public static boolean mayReferToShadow(EObject context) {
		var definitionContext = EcoreUtil2.getContainerOfType(context, ParametricDefinition.class);
		return switch (definitionContext) {
			case PredicateDefinition predicateDefinition -> predicateDefinition.getKind() == PredicateKind.SHADOW;
			case RuleDefinition ignored -> true;
			case null, default -> false;
		};
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

	public static boolean isInvalidMultiplicityConstraint(Relation relation) {
		return relation.eContainingFeature() == ProblemPackage.Literals.REFERENCE_DECLARATION__INVALID_MULTIPLICITY;
	}

	public static boolean isComputedValuePredicate(Relation relation) {
		return relation.eContainingFeature() == ProblemPackage.Literals.PREDICATE_DEFINITION__COMPUTED_VALUE;
	}

	public static boolean hasMultiplicityConstraint(ReferenceDeclaration referenceDeclaration) {
		if (referenceDeclaration.getReferenceType() instanceof DatatypeDeclaration) {
			return false;
		}
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

	public static boolean isDerivedStatePredicate(EObject predicateDefinition) {
		var containingFeature = predicateDefinition.eContainingFeature();
		return containingFeature == ProblemPackage.Literals.REFERENCE_DECLARATION__INVALID_MULTIPLICITY ||
				containingFeature == ProblemPackage.Literals.PREDICATE_DEFINITION__COMPUTED_VALUE;
	}

	public static boolean isBasePredicate(PredicateDefinition predicateDefinition) {
		if (isBuiltIn(predicateDefinition) || predicateDefinition == null) {
			// Built-in predicates have no clauses, but are not base.
			return false;
		}
		return predicateDefinition.getKind() == PredicateKind.DEFAULT && predicateDefinition.getBodies().isEmpty();
	}

	public static boolean hasComputedValue(PredicateDefinition predicateDefinition) {
		return predicateDefinition.getKind() != PredicateKind.SHADOW && !isBasePredicate(predicateDefinition) &&
				!isBuiltIn(predicateDefinition);
	}

	public static boolean isTypeLike(Relation relation) {
		return getArityWithoutProxyResolution(relation) == 1;
	}

	public static boolean isContainmentReference(ReferenceDeclaration referenceDeclaration) {
		return referenceDeclaration.getKind() == ReferenceKind.CONTAINMENT;
	}

	public static boolean isContainerReference(ReferenceDeclaration referenceDeclaration) {
		var kind = referenceDeclaration.getKind();
		if (kind == null) {
			return false;
		}
		return switch (kind) {
			case CONTAINMENT -> false;
			case CONTAINER -> true;
			case DEFAULT, REFERENCE -> {
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

	public static boolean parameterBindingAnnotationsAreForbidden(RuleDefinition ruleDefinition) {
		var kind = ruleDefinition.getKind();
		return kind != RuleKind.DECISION && kind != RuleKind.CONCRETIZATION;
	}

	public static int getArityWithoutProxyResolution(Relation relation) {
		return switch (relation) {
			case ClassDeclaration ignoredClassDeclaration -> 1;
			case EnumDeclaration ignoredEnumDeclaration -> 1;
			case DatatypeDeclaration ignoredDatatypeDeclaration -> 1;
			case ReferenceDeclaration ignoredReferenceDeclaration -> 2;
			case PredicateDefinition predicateDefinition -> predicateDefinition.getParameters().size();
			default -> throw new IllegalArgumentException("Unknown Relation: " + relation);
		};
	}

	public static boolean isConcretizeByDefault(Relation relation) {
		return switch (relation) {
			case ClassDeclaration ignored -> true;
			case ReferenceDeclaration ignored -> true;
			case PredicateDefinition predicateDefinition -> isBasePredicate(predicateDefinition);
			default -> false;
		};
	}

	public static boolean canEnableConcretization(Relation relation) {
		return isConcretizeByDefault(relation);
	}

	public static boolean canDisableConcretization(Relation relation) {
		if (relation instanceof ReferenceDeclaration referenceDeclaration &&
				(isContainmentReference(referenceDeclaration) || isContainerReference(referenceDeclaration))) {
			return false;
		}
		if (relation instanceof ClassDeclaration) {
			return false;
		}
		return isConcretizeByDefault(relation);
	}

	public static boolean isDecideByDefault(Relation relation) {
		return isConcretizeByDefault(relation);
	}

	public static boolean canEnableDecision(Relation relation) {
		return isDecideByDefault(relation);
	}

	public static boolean canDisableDecision(Relation relation) {
		return isDecideByDefault(relation);
	}
}
