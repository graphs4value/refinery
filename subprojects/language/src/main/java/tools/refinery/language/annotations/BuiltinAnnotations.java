/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.utils.ProblemUtil;

import java.util.List;

public class BuiltinAnnotations extends DeclarativeAnnotationValidator {
	public static final QualifiedName OPTIONAL = BuiltinLibrary.BUILTIN_ANNOTATIONS_LIBRARY_NAME.append(
			"optional");
	public static final QualifiedName REPEATABLE = BuiltinLibrary.BUILTIN_ANNOTATIONS_LIBRARY_NAME.append(
			"repeatable");
	public static final QualifiedName FOCUS = BuiltinLibrary.BUILTIN_STRATEGY_LIBRARY_NAME.append("focus");
	public static final QualifiedName LONE = BuiltinLibrary.BUILTIN_STRATEGY_LIBRARY_NAME.append("lone");
	public static final QualifiedName MULTI = BuiltinLibrary.BUILTIN_STRATEGY_LIBRARY_NAME.append("multi");
	public static final QualifiedName CONCRETIZE = BuiltinLibrary.BUILTIN_STRATEGY_LIBRARY_NAME.append(
			"concretize");
	public static final String CONCRETIZE_AUTO = "auto";
	public static final QualifiedName DECIDE = BuiltinLibrary.BUILTIN_STRATEGY_LIBRARY_NAME.append(
			"decide");
	public static final String DECIDE_AUTO = CONCRETIZE_AUTO;

	private static final List<QualifiedName> BINDING_MODES = List.of(FOCUS, LONE, MULTI);

	@ValidateAnnotation("OPTIONAL")
	private void validateOptional(Annotation annotation) {
		var annotatedElement = annotation.getAnnotatedElement();
		if (!isParameter(annotatedElement, AnnotationDeclaration.class)) {
			error("@optional can only be applied to annotation parameters.", annotation);
		}
	}

	@ValidateAnnotation("REPEATABLE")
	private void validateRepeatable(Annotation annotation) {
		var annotatedElement = annotation.getAnnotatedElement();
		if (!(annotatedElement instanceof AnnotationDeclaration) &&
				!isParameter(annotatedElement, AnnotationDeclaration.class)) {
			error("@repeatable can only be applied to annotation declarations and annotation parameters.", annotation);
		}
	}

	@ValidateAnnotation("FOCUS")
	@ValidateAnnotation("LONE")
	@ValidateAnnotation("MULTI")
	private void validateBindingMode(Annotation annotation) {
		var annotatedElement = annotation.getAnnotatedElement();
		if (!isParameter(annotatedElement, RuleDefinition.class)) {
			error("@%s can only be applied to rule parameters."
					.formatted(annotation.getAnnotationName().getLastSegment()), annotation);
			return;
		}
		var annotations = annotationsFor(annotatedElement);
		long bindingModeCount = BINDING_MODES.stream()
				.filter(annotations::hasAnnotation)
				.count();
		if (bindingModeCount >= 2) {
			error("Only one of @focus, @lone, or @multi can be applied to a rule parameter at a time.",
					annotation);
		}
	}

	// It only makes sense for this method to do the positive check.
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private static boolean isParameter(EObject annotatedElement,
									   Class<? extends ParametricDefinition> definitionType) {
		return annotatedElement instanceof Parameter &&
				definitionType.isInstance(EcoreUtil2.getContainerOfType(annotatedElement, ParametricDefinition.class));
	}

	@ValidateAnnotation("CONCRETIZE")
	private void validateConcretize(Annotation annotation) {
		if (!(annotation.getAnnotatedElement() instanceof Relation relation)) {
			error("@concretize can only be applied to logical relations.", annotation);
			return;
		}
		boolean value = annotation.getBoolean(CONCRETIZE_AUTO).orElse(true);
		if (value) {
			if (!ProblemUtil.canEnableConcretization(relation)) {
				error("Automatic concretization can't be enabled for '%s'.".formatted(relation.getName()), annotation);
				return;
			}
		} else if (!ProblemUtil.canDisableConcretization(relation)) {
			error("Automatic concretization can't be disabled for '%s'.".formatted(relation.getName()), annotation);
			return;
		}
		if (value == ProblemUtil.isConcretizeByDefault(relation)) {
			warning("Automatic concretization for '%s' is already %s."
							.formatted(relation.getName(), value ? "enabled" : "disabled"), annotation);
		}
	}

	@ValidateAnnotation("DECIDE")
	private void validateDecide(Annotation annotation) {
		if (!(annotation.getAnnotatedElement() instanceof Relation relation)) {
			error("@decide can only be applied to logical relations.", annotation);
			return;
		}
		boolean value = annotation.getBoolean(DECIDE_AUTO).orElse(true);
		if (value) {
			if (!ProblemUtil.canEnableDecision(relation)) {
				error("Automatic decision can't be enabled for '%s'.".formatted(relation.getName()), annotation);
				return;
			}
		} else if (!ProblemUtil.canDisableDecision(relation)) {
			error("Automatic decision can't be disabled for '%s'.".formatted(relation.getName()), annotation);
			return;
		}
		var concretize = annotationsFor(relation).getAnnotation(CONCRETIZE)
				.flatMap(a -> a.getBoolean(CONCRETIZE_AUTO));
		boolean defaultValue;
		if (concretize.isPresent() && Boolean.FALSE.equals(concretize.get())) {
			defaultValue = false;
		} else {
			defaultValue = ProblemUtil.isDecideByDefault(relation);
		}
		if (value == defaultValue) {
			warning("Automatic decision for '%s' is already %s."
					.formatted(relation.getName(), value ? "enabled" : "disabled"), annotation);
		}
	}
}
