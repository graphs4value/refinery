/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations.internal;

import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.model.problem.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalInt;

public final class AnnotationUtil {
	static final String REPEATABLE_NAME = "repeatable";
	static final String OPTIONAL_NAME = "optional";

	private AnnotationUtil() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	static boolean isRepeatable(AnnotationDeclaration annotationDeclaration) {
		return hasMetaAnnotation(annotationDeclaration, REPEATABLE_NAME);
	}

	static boolean isOptional(Parameter parameter) {
		return hasMetaAnnotation(parameter, OPTIONAL_NAME);
	}

	static boolean isRepeatable(Parameter parameter) {
		return hasMetaAnnotation(parameter, REPEATABLE_NAME);
	}

	/**
	 * Checks if the given annotated element has the given annotation without processing the rest of its annotations.
	 * <p>
	 * This method is used to check whether an {@link AnnotationDeclaration} or
	 * {@link Parameter} is marked as repeatable during processing by the
	 * {@link TypedAnnotationContext}, when the result of the annotation processing is not yet available.
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

	public static Optional<Relation> toRelation(Expr value) {
		if (value instanceof VariableOrNodeExpr variableOrNodeExpr) {
			return Optional.ofNullable(variableOrNodeExpr.getRelation());
		}
		return Optional.empty();
	}

	public static Optional<Node> toNode(Expr value) {
		if (value instanceof VariableOrNodeExpr variableOrNodeExpr &&
				variableOrNodeExpr.getVariableOrNode() instanceof Node node) {
			return Optional.of(node);
		}
		return Optional.empty();
	}

	public static Optional<Boolean> toBoolean(Expr value) {
		if (value instanceof LogicConstant logicConstant) {
			return switch (logicConstant.getLogicValue()) {
				case TRUE -> Optional.of(true);
				case FALSE -> Optional.of(false);
				case null, default -> Optional.empty();
			};
		}
		return Optional.empty();
	}

	public static OptionalInt toInteger(Expr value) {
		return switch (value) {
			case IntConstant intConstant -> OptionalInt.of(intConstant.getIntValue());
			case ArithmeticUnaryExpr unaryExpr -> {
				if (!(unaryExpr.getBody() instanceof IntConstant intConstant)) {
					yield OptionalInt.empty();
				}
				int intValue = intConstant.getIntValue();
				yield switch (unaryExpr.getOp()) {
					case PLUS -> OptionalInt.of(intValue);
					case MINUS -> OptionalInt.of(-intValue);
					case null -> OptionalInt.empty();
				};
			}
			case null, default -> OptionalInt.empty();
		};
	}

	public static Optional<BigDecimal> toBigDecimal(Expr value) {
		return switch (value) {
			case RealConstant realConstant -> Optional.of(realConstant.getRealValue());
			case ArithmeticUnaryExpr unaryExpr -> {
				if (!(unaryExpr.getBody() instanceof RealConstant realConstant)) {
					yield Optional.empty();
				}
				var realValue = realConstant.getRealValue();
				yield switch (unaryExpr.getOp()) {
					case PLUS -> Optional.of(realValue);
					case MINUS -> Optional.of(realValue.multiply(BigDecimal.valueOf(-1)));
					case null -> Optional.empty();
				};
			}
			case null, default -> Optional.empty();
		};
	}

	public static Optional<String> toString(Expr value) {
		if (value instanceof StringConstant stringConstant) {
			return Optional.ofNullable(stringConstant.getStringValue());
		}
		return Optional.empty();
	}
}
