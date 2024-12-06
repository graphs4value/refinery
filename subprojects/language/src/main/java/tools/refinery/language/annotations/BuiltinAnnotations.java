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


public class BuiltinAnnotations extends DeclarativeAnnotationValidator {
	public static final QualifiedName OPTIONAL = BuiltinLibrary.BUILTIN_ANNOTATIONS_LIBRARY_NAME.append(
			"optional");
	public static final QualifiedName REPEATABLE = BuiltinLibrary.BUILTIN_ANNOTATIONS_LIBRARY_NAME.append(
			"repeatable");


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

	// It only makes sense for this method to do the positive check.
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private static boolean isParameter(EObject annotatedElement,
									   Class<? extends ParametricDefinition> definitionType) {
		return annotatedElement instanceof Parameter &&
				definitionType.isInstance(EcoreUtil2.getContainerOfType(annotatedElement, ParametricDefinition.class));
	}
}
