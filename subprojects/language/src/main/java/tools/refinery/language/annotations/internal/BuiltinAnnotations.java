/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations.internal;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.annotations.Annotation;
import tools.refinery.language.annotations.DeclarativeAnnotationValidator;
import tools.refinery.language.annotations.ValidateAnnotation;
import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.model.problem.AnnotationDeclaration;
import tools.refinery.language.model.problem.Parameter;
import tools.refinery.language.model.problem.ParametricDefinition;

public class BuiltinAnnotations extends DeclarativeAnnotationValidator {
	public static final QualifiedName OPTIONAL = BuiltinLibrary.BUILTIN_ANNOTATIONS_LIBRARY_NAME.append(
			"optional");
	public static final QualifiedName REPEATABLE = BuiltinLibrary.BUILTIN_ANNOTATIONS_LIBRARY_NAME.append(
			"repeatable");

	@ValidateAnnotation("OPTIONAL")
	public void validateOptional(Annotation annotation) {
		var annotatedElement = annotation.getAnnotatedElement();
		if (!isAnnotationParameter(annotatedElement)) {
			error("@optional can only be applied to annotation parameters.", annotation);
		}
	}

	@ValidateAnnotation("REPEATABLE")
	public void validateRepeatable(Annotation annotation) {
		var annotatedElement = annotation.getAnnotatedElement();
		if (!(annotatedElement instanceof AnnotationDeclaration) && !isAnnotationParameter(annotatedElement)) {
			error("@repeatable can only be applied to annotation declarations and annotation parameters.", annotation);
		}
	}

	// It only makes sense for this method to do the positive check.
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private static boolean isAnnotationParameter(EObject annotatedElement) {
		return annotatedElement instanceof Parameter &&
				EcoreUtil2.getContainerOfType(annotatedElement,
						ParametricDefinition.class) instanceof AnnotationDeclaration;
	}
}
