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
import tools.refinery.language.model.problem.AnnotationDeclaration;
import tools.refinery.language.model.problem.Parameter;
import tools.refinery.language.model.problem.ParametricDefinition;
import tools.refinery.language.model.problem.RuleDefinition;

import java.util.List;

public class BuiltinAnnotations extends DeclarativeAnnotationValidator {
	public static final QualifiedName FOCUS = BuiltinLibrary.BUILTIN_LIBRARY_NAME.append("focus");
	public static final QualifiedName LONE = BuiltinLibrary.BUILTIN_LIBRARY_NAME.append("lone");
	public static final QualifiedName MULTI = BuiltinLibrary.BUILTIN_LIBRARY_NAME.append("multi");
	public static final QualifiedName OPTIONAL = BuiltinLibrary.BUILTIN_ANNOTATIONS_LIBRARY_NAME.append(
			"optional");
	public static final QualifiedName REPEATABLE = BuiltinLibrary.BUILTIN_ANNOTATIONS_LIBRARY_NAME.append(
			"repeatable");

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
}
