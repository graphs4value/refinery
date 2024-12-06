/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.utils;

import com.google.inject.Inject;
import tools.refinery.language.annotations.AnnotationContext;
import tools.refinery.language.annotations.BuiltinAnnotations;
import tools.refinery.language.model.problem.Parameter;

public class BuiltinAnnotationContext {
	@Inject
	private AnnotationContext annotationContext;

	public ParameterBinding getParameterBinding(Parameter parameter) {
		var annotations = annotationContext.annotationsFor(parameter);
		if (annotations.hasAnnotation(BuiltinAnnotations.FOCUS)) {
			return ParameterBinding.FOCUS;
		}
		if (annotations.hasAnnotation(BuiltinAnnotations.LONE)) {
			return ParameterBinding.LONE;
		}
		if (annotations.hasAnnotation(BuiltinAnnotations.MULTI)) {
			return ParameterBinding.MULTI;
		}
		return ParameterBinding.SINGLE;
	}
}
