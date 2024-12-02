/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.jetbrains.annotations.NotNull;
import tools.refinery.language.model.problem.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Annotation {
	private final EObject annotatedElement;
	private final QualifiedName annotationName;
	private final tools.refinery.language.model.problem.Annotation problemAnnotation;
	private final Map<String, CollectedArguments> arguments;

	Annotation(EObject annotatedElement, QualifiedName annotationName,
			   tools.refinery.language.model.problem.Annotation problemAnnotation) {
		this.annotatedElement = annotatedElement;
		this.annotationName = annotationName;
		this.problemAnnotation = problemAnnotation;
		var declaration = problemAnnotation.getDeclaration();
		if (declaration == null) {
			throw new IllegalArgumentException("Expected Annotation with AnnotationDeclaration");
		}
		var collector = new ArgumentListCollector(declaration.getParameters());
		collector.processArgumentList(problemAnnotation.getArguments());
		arguments = collector.getCollectedArguments();
	}

	public EObject getAnnotatedElement() {
		return annotatedElement;
	}

	public tools.refinery.language.model.problem.Annotation getAnnotation() {
		return problemAnnotation;
	}

	public QualifiedName getAnnotationName() {
		return annotationName;
	}

	@NotNull
	private CollectedArguments getCollectedArguments(String parameterName) {
		var collected = arguments.get(parameterName);
		if (collected == null) {
			throw new IllegalArgumentException("No such argument '%s' for annotation '%s'."
					.formatted(parameterName, annotationName));
		}
		return collected;
	}

	public Optional<Expr> getValue(String parameterName) {
		var collected = getCollectedArguments(parameterName);
		if (collected.repeatable()) {
			throw new IllegalArgumentException("Argument '%s' of annotation '%s' is repeatable."
					.formatted(parameterName, annotationName));
		}
		var values = collected.values();
		return values.isEmpty() ? Optional.empty() : Optional.ofNullable(values.getFirst());
	}

	public Stream<Expr> getValues(String parameterName) {
		var collected = getCollectedArguments(parameterName);
		return collected.values().stream().filter(Objects::nonNull);
	}

	private <T> Optional<T> transformValue(String parameterName, Function<Expr, Optional<T>> transformer) {
		return getValue(parameterName).flatMap(transformer);
	}

	private <T> Stream<T> transformValues(String parameterName, Function<Expr, Optional<T>> transformer) {
		return getValues(parameterName).map(transformer).mapMulti(Optional::ifPresent);
	}

	public Optional<Relation> getRelation(String parameterName) {
		return transformValue(parameterName, this::toRelation);
	}

	public Stream<Relation> getRelations(String parameterName) {
		return transformValues(parameterName, this::toRelation);
	}

	private Optional<Relation> toRelation(Expr value) {
		if (value instanceof VariableOrNodeExpr variableOrNodeExpr) {
			return Optional.ofNullable(variableOrNodeExpr.getRelation());
		}
		return Optional.empty();
	}

	public Optional<Node> getNode(String parameterName) {
		return transformValue(parameterName, this::toNode);
	}

	public Stream<Node> getNodes(String parameterName) {
		return transformValues(parameterName, this::toNode);
	}

	private Optional<Node> toNode(Expr value) {
		if (value instanceof VariableOrNodeExpr variableOrNodeExpr &&
				variableOrNodeExpr.getVariableOrNode() instanceof Node node) {
			return Optional.of(node);
		}
		return Optional.empty();
	}

	public Optional<Boolean> getBoolean(String parameterName) {
		return transformValue(parameterName, this::toBoolean);
	}

	public Stream<Boolean> getBooleans(String parameterName) {
		return transformValues(parameterName, this::toBoolean);
	}

	private Optional<Boolean> toBoolean(Expr value) {
		if (value instanceof LogicConstant logicConstant) {
			return switch (logicConstant.getLogicValue()) {
				case TRUE -> Optional.of(true);
				case FALSE -> Optional.of(false);
				case null, default -> Optional.empty();
			};
		}
		return Optional.empty();
	}

	public OptionalInt getInteger(String parameterName) {
		var value = getValue(parameterName);
		if (value.isPresent()) {
			return toInteger(value.get());
		}
		return OptionalInt.empty();
	}

	public IntStream getIntegers(String parameterName) {
		return getValues(parameterName).map(this::toInteger).mapMultiToInt(OptionalInt::ifPresent);
	}

	private OptionalInt toInteger(Expr value) {
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

	public OptionalDouble getDouble(String parameterName) {
		var value = getValue(parameterName);
		if (value.isPresent()) {
			return toDouble(value.get());
		}
		return OptionalDouble.empty();
	}

	public DoubleStream getDoubles(String parameterName) {
		return getValues(parameterName).map(this::toDouble).mapMultiToDouble(OptionalDouble::ifPresent);
	}

	private OptionalDouble toDouble(Expr value) {
		return switch (value) {
			case RealConstant realConstant -> OptionalDouble.of(realConstant.getRealValue());
			case ArithmeticUnaryExpr unaryExpr -> {
				if (!(unaryExpr.getBody() instanceof RealConstant realConstant)) {
					yield OptionalDouble.empty();
				}
				double realValue = realConstant.getRealValue();
				yield switch (unaryExpr.getOp()) {
					case PLUS -> OptionalDouble.of(realValue);
					case MINUS -> OptionalDouble.of(-realValue);
					case null -> OptionalDouble.empty();
				};
			}
			case null, default -> OptionalDouble.empty();
		};
	}

	public Optional<String> getString(String parameterName) {
		return transformValue(parameterName, this::toString);
	}

	public Stream<String> getStrings(String parameterName) {
		return transformValues(parameterName, this::toString);
	}

	private Optional<String> toString(Expr value) {
		if (value instanceof StringConstant stringConstant) {
			return Optional.ofNullable(stringConstant.getStringValue());
		}
		return Optional.empty();
	}
}
