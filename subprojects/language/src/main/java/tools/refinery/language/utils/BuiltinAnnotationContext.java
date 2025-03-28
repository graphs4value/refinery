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
import tools.refinery.language.model.problem.Relation;
import tools.refinery.language.model.problem.RuleDefinition;

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

	public ConcretizationSettings getConcretizationSettings(Relation relation) {
		var annotations = annotationContext.annotationsFor(relation);
		var concretize = annotations.getAnnotation(BuiltinAnnotations.CONCRETIZE)
				.flatMap(annotation -> annotation.getBoolean(BuiltinAnnotations.CONCRETIZE_AUTO));
		var decide = annotations.getAnnotation(BuiltinAnnotations.DECIDE)
				.flatMap(annotation -> annotation.getBoolean(BuiltinAnnotations.DECIDE_AUTO));
		return new ConcretizationSettings(concretize.orElseGet(() -> ProblemUtil.isConcretizeByDefault(relation)),
				decide.orElseGet(() -> {
					if (concretize.isPresent() && Boolean.FALSE.equals(concretize.get())) {
						return false;
					}
					return ProblemUtil.isDecideByDefault(relation);
				}));
	}

	public DecisionSettings getDecisionSettings(RuleDefinition ruleDefinition) {
		var annotations = annotationContext.annotationsFor(ruleDefinition);
		var priorityAnnotation = annotations.getAnnotation(BuiltinAnnotations.PRIORITY);
		int priority = priorityAnnotation.map(annotation -> annotation.getInteger(BuiltinAnnotations.PRIORITY_VALUE)
						.orElse(DecisionSettings.DEFAULT_PRIORITY))
				.orElse(DecisionSettings.DEFAULT_PRIORITY);
		var weighAnnotation = annotations.getAnnotation(BuiltinAnnotations.WEIGHT);
		if (weighAnnotation.isEmpty()) {
			return new DecisionSettings(priority);
		}
		double coefficient = weighAnnotation.get()
				.getDouble(BuiltinAnnotations.WEIGHT_COEFFICIENT)
				.orElse(DecisionSettings.DEFAULT_COEFFICIENT);
		double exponent = weighAnnotation.get()
				.getDouble(BuiltinAnnotations.WEIGHT_EXPONENT)
				.orElse(DecisionSettings.DEFAULT_EXPONENT);
		return new DecisionSettings(priority, coefficient, exponent);
	}
}
