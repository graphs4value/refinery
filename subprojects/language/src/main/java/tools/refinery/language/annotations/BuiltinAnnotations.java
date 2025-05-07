/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.documentation.DocumentationCommentParser;
import tools.refinery.language.documentation.TypeHashProvider;
import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.utils.BuiltinAnnotationContext;
import tools.refinery.language.utils.DecisionSettings;
import tools.refinery.language.utils.ProblemUtil;
import tools.refinery.language.validation.ClassHierarchyCollector;

import java.util.List;

public class BuiltinAnnotations extends DeclarativeAnnotationValidator {
	public static final QualifiedName OPTIONAL = BuiltinLibrary.BUILTIN_ANNOTATIONS_LIBRARY_NAME.append(
			"optional");
	public static final QualifiedName REPEATABLE = BuiltinLibrary.BUILTIN_ANNOTATIONS_LIBRARY_NAME.append(
			"repeatable");
	public static final QualifiedName FOCUS = BuiltinLibrary.BUILTIN_STRATEGY_LIBRARY_NAME.append("focus");
	public static final QualifiedName LONE = BuiltinLibrary.BUILTIN_STRATEGY_LIBRARY_NAME.append("lone");
	public static final QualifiedName MULTI = BuiltinLibrary.BUILTIN_STRATEGY_LIBRARY_NAME.append("multi");
	public static final QualifiedName CONCRETIZE = BuiltinLibrary.BUILTIN_STRATEGY_LIBRARY_NAME.append("concretize");
	public static final String CONCRETIZE_AUTO = "auto";
	public static final QualifiedName DECIDE = BuiltinLibrary.BUILTIN_STRATEGY_LIBRARY_NAME.append("decide");
	public static final String DECIDE_AUTO = CONCRETIZE_AUTO;
	public static final QualifiedName PRIORITY = BuiltinLibrary.BUILTIN_STRATEGY_LIBRARY_NAME.append("priority");
	public static final String PRIORITY_VALUE = "value";
	public static final QualifiedName WEIGHT = BuiltinLibrary.BUILTIN_STRATEGY_LIBRARY_NAME.append("weight");
	public static final String WEIGHT_COEFFICIENT = "coefficient";
	public static final String WEIGHT_EXPONENT = "exponent";
	public static final QualifiedName COLOR = BuiltinLibrary.BUILTIN_VIEW_LIBRARY_NAME.append("color");
	public static final String COLOR_COLOR_ID = "colorId";
	public static final String COLOR_HEX = "hex";
	public static final QualifiedName SHOW = BuiltinLibrary.BUILTIN_VIEW_LIBRARY_NAME.append("show");
	public static final String SHOW_HIDE_UNKNOWN = "hideUnknown";
	public static final QualifiedName HIDE = BuiltinLibrary.BUILTIN_VIEW_LIBRARY_NAME.append("hide");

	private static final List<QualifiedName> BINDING_MODES = List.of(FOCUS, LONE, MULTI);
	private static final List<QualifiedName> VISIBILITIES = List.of(SHOW, HIDE);

	@Inject
	private BuiltinAnnotationContext builtinAnnotationContext;

	@Inject
	private ClassHierarchyCollector classHierarchyCollector;

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
					.formatted(relation.getName(), flagToEnabledState(value)), annotation);
		}
	}

	private static String flagToEnabledState(boolean value) {
		return value ? "enabled" : "disabled";
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
		if (relation instanceof ClassDeclaration classDeclaration) {
			validateClassDeclarationDecide(annotation, classDeclaration);
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
					.formatted(relation.getName(), flagToEnabledState(value)), annotation);
		}
	}

	private void validateClassDeclarationDecide(Annotation annotation, ClassDeclaration classDeclaration) {
		var superTypes = classHierarchyCollector.getSuperTypes(classDeclaration);
		boolean inheritedDecide = ProblemUtil.isDecideByDefault(classDeclaration);
		ClassDeclaration inheritedFrom = null;
		for (var superType : superTypes) {
			if (!classDeclaration.equals(superType) && superType instanceof ClassDeclaration superClassDeclaration &&
					!builtinAnnotationContext.isClassDeclarationDecide(superClassDeclaration)) {
				inheritedDecide = false;
				inheritedFrom = superClassDeclaration;
				break;
			}
		}
		boolean value = annotation.getBoolean(DECIDE_AUTO).orElse(true);
		if (value == inheritedDecide) {
			var messageBuilder = new StringBuilder()
					.append("Automatic decision for '")
					.append(classDeclaration.getName())
					.append("' is already ")
					.append(flagToEnabledState(value));
			if (inheritedFrom != null) {
				messageBuilder.append(", because it was inherited from '")
						.append(inheritedFrom.getName())
						.append("'");
			}
			messageBuilder.append(".");
			warning(messageBuilder.toString(), annotation);
			return;
		}
		if (value && inheritedFrom != null) {
			// {@code inheritedDecide} is {@code false} here, because we already know that it differs from
			// {@code decide}.
			var message = "Automatic decision can't be enabled for '%s', because it was disabled for superclass '%s'."
					.formatted(classDeclaration.getName(), inheritedFrom.getName());
			error(message, annotation);
		}
	}

	@ValidateAnnotation("PRIORITY")
	@ValidateAnnotation("WEIGHT")
	private void validateDecisionRuleAnnotation(Annotation annotation) {
		if (!(annotation.getAnnotatedElement() instanceof RuleDefinition ruleDefinition) ||
				!RuleKind.DECISION.equals(ruleDefinition.getKind())) {
			var message = "@%s can only be applied to decision rules."
					.formatted(annotation.getAnnotation().getDeclaration().getName());
			error(message, annotation);
		}
	}

	@ValidateAnnotation("PRIORITY")
	private void validatePriority(Annotation annotation) {
		var value = annotation.getInteger(PRIORITY_VALUE).orElse(DecisionSettings.DEFAULT_PRIORITY);
		if (value == DecisionSettings.DEFAULT_PRIORITY) {
			var message = "Priority is already at its default value (%d)."
					.formatted(DecisionSettings.DEFAULT_PRIORITY);
			warning(message, annotation);
		}
	}

	@ValidateAnnotation("WEIGHT")
	private void validateWeight(Annotation annotation) {
		var coefficient = annotation.getDouble(WEIGHT_COEFFICIENT);
		var exponent = annotation.getDouble(WEIGHT_EXPONENT);
		if (coefficient.isEmpty() && exponent.isEmpty()) {
			var message = "Must set either weight %s or %s.".formatted(WEIGHT_COEFFICIENT, WEIGHT_EXPONENT);
			error(message, annotation);
			return;
		}
		if (coefficient.orElse(DecisionSettings.DEFAULT_COEFFICIENT) <= 0) {
			var message = "Weight coefficient must be positive.";
			error(message, annotation);
		}
		if (exponent.orElse(DecisionSettings.DEFAULT_EXPONENT) < 0) {
			var message = "Weight exponent must be non-negative.";
			error(message, annotation);
		}
	}

	@ValidateAnnotation("COLOR")
	private void validateColor(Annotation annotation) {
		if (!(annotation.getAnnotatedElement() instanceof ClassDeclaration)) {
			error("Only class declarations can be colored.", annotation);
			return;
		}
		var colorId = annotation.getInteger(COLOR_COLOR_ID);
		var hex = annotation.getString(COLOR_HEX);
		if (colorId.isEmpty() && hex.isEmpty()) {
			error("Must set either %s or %s.".formatted(COLOR_COLOR_ID, COLOR_HEX), annotation);
		}
		if (colorId.isPresent() && hex.isPresent()) {
			error("Can't set %s and %s at the same time.".formatted(COLOR_COLOR_ID, COLOR_HEX), annotation);
		}
		colorId.ifPresent(value -> {
			if (value < 0 || value >= TypeHashProvider.COLOR_COUNT) {
				var message = "Color ID must be a positive integer between 0 and %d."
						.formatted(TypeHashProvider.COLOR_COUNT - 1);
				error(message, annotation);
			}
		});
		hex.ifPresent(value -> {
			if (!DocumentationCommentParser.isValidHex(value)) {
				error("Color hex must be a valid HTML hex color code.", annotation);
			}
		});
	}

	@ValidateAnnotation("SHOW")
	@ValidateAnnotation("HIDE")
	private void validateVisibility(Annotation annotation) {
		var annotatedElement = annotation.getAnnotatedElement();
		var annotations = annotationsFor(annotatedElement);
		long visibilityCount = VISIBILITIES.stream()
				.filter(annotations::hasAnnotation)
				.count();
		if (visibilityCount >= 2) {
			error("Only one of @show or @hide can be applied to a relation at a time.", annotation);
		}
		if (!(annotatedElement instanceof Relation relation)) {
			error("Only relations can be shown or hidden.", annotation);
			return;
		}
		int arity = ProblemUtil.getArityWithoutProxyResolution(relation);
		if (arity < 1 || arity > 2) {
			error("Only unary (type) or binary (edge) relations can be shown or hidden.", annotation);
		}
	}
}
