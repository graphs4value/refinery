/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.model.problem.AnnotatedElement;
import tools.refinery.language.model.problem.AnnotationDeclaration;
import tools.refinery.language.model.problem.Parameter;

final class AnnotationUtil {
	static final String REPEATABLE_NAME = "repeatable";
	static final String OPTIONAL_NAME = "optional";

	private AnnotationUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static boolean isRepeatable(AnnotationDeclaration annotationDeclaration) {
		return hasMetaAnnotation(annotationDeclaration, REPEATABLE_NAME);
	}

	public static boolean isOptional(Parameter parameter) {
		return hasMetaAnnotation(parameter, OPTIONAL_NAME);
	}

	public static boolean isRepeatable(Parameter parameter) {
		return hasMetaAnnotation(parameter, REPEATABLE_NAME);
	}

	/**
	 * Checks if the given annotated element has the given annotation without processing the rest of its annotations.
	 * <p>
	 * This method is used to check whether an {@link AnnotationDeclaration} or
	 * {@link tools.refinery.language.model.problem.Parameter} is marked as repeatable during processing by the
	 * {@link AnnotationContext}, when the result of the annotation processing is not yet available.
	 * </p>
	 *
	 * @param annotatedElement   The annotated element to check for the {@code repeatable} annotation.
	 * @param metaAnnotationName The name of the annotation in the {@code builtin::annotations} library to check for.
	 * @return {@code true} if {@code annotatedElement} has the {@code metaAnnotationName} annotation.
	 */
	private static boolean hasMetaAnnotation(AnnotatedElement annotatedElement, String metaAnnotationName) {
		for (var annotation : annotatedElement.getAnnotations().getAnnotations()) {
			var declaration = annotation.getDeclaration();
			if (declaration != null && metaAnnotationName.equals(declaration.getName()) &&
					BuiltinLibrary.BUILTIN_ANNOTATIONS_LIBRARY_URI.equals(declaration.eResource().getURI())) {
				return true;
			}
		}
		return false;
	}
}
