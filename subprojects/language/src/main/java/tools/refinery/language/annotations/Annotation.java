/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.annotations;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.refinery.language.annotations.internal.AnnotationUtil;
import tools.refinery.language.model.problem.Expr;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.Relation;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface Annotation {
	@Nullable
	EObject getAnnotatedElement();

	@NotNull
	tools.refinery.language.model.problem.Annotation getAnnotation();

	@NotNull
	QualifiedName getAnnotationName();

	Optional<Expr> getValue(String parameterName);

	Stream<Expr> getValues(String parameterName);

	default Optional<Relation> getRelation(String parameterName) {
		return getValue(parameterName).flatMap(AnnotationUtil::toRelation);
	}

	default Stream<Relation> getRelations(String parameterName) {
		return getValues(parameterName).map(AnnotationUtil::toRelation).mapMulti(Optional::ifPresent);
	}

	default Optional<Node> getNode(String parameterName) {
		return getValue(parameterName).flatMap(AnnotationUtil::toNode);
	}

	default Stream<Node> getNodes(String parameterName) {
		return getValues(parameterName).map(AnnotationUtil::toNode).mapMulti(Optional::ifPresent);
	}

	default Optional<Boolean> getBoolean(String parameterName) {
		return getValue(parameterName).flatMap(AnnotationUtil::toBoolean);
	}

	default Stream<Boolean> getBooleans(String parameterName) {
		return getValues(parameterName).map(AnnotationUtil::toBoolean).mapMulti(Optional::ifPresent);
	}

	default OptionalInt getInteger(String parameterName) {
		var value = getValue(parameterName);
		if (value.isPresent()) {
			return AnnotationUtil.toInteger(value.get());
		}
		return OptionalInt.empty();
	}

	default IntStream getIntegers(String parameterName) {
		return getValues(parameterName).map(AnnotationUtil::toInteger).mapMultiToInt(OptionalInt::ifPresent);
	}

	default OptionalDouble getDouble(String parameterName) {
		var value = getValue(parameterName);
		if (value.isPresent()) {
			return AnnotationUtil.toDouble(value.get());
		}
		return OptionalDouble.empty();
	}

	default DoubleStream getDoubles(String parameterName) {
		return getValues(parameterName).map(AnnotationUtil::toDouble).mapMultiToDouble(OptionalDouble::ifPresent);
	}

	default Optional<String> getString(String parameterName) {
		return getValue(parameterName).flatMap(AnnotationUtil::toString);
	}

	default Stream<String> getStrings(String parameterName) {
		return getValues(parameterName).map(AnnotationUtil::toString).mapMulti(Optional::ifPresent);
	}
}
