/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.ibex;

import com.google.inject.Inject;
import org.eclipse.collections.api.factory.primitive.ObjectDoubleMaps;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.annotations.AnnotationContext;
import tools.refinery.language.annotations.Annotations;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.theory.TheoryProvider;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.theory.Theory;

import java.math.BigDecimal;
import java.util.Optional;

import static tools.refinery.store.reasoning.ibex.IbexPropagator.DEFAULT_PRECISION;
import static tools.refinery.store.reasoning.ibex.IbexPropagator.DEFAULT_RELATIVE_EPSILON;

public class IbexTheoryProvider implements TheoryProvider {
	public static final QualifiedName IBEX_THEORY = IbexLibrary.IBEX_LIBRARY.append("ibex");

	@Inject
	private AnnotationContext annotationContext;

	@Override
	public Optional<Theory> createTheory(QualifiedName theoryName, Annotations annotations, ProblemTrace trace) {
		if (!IBEX_THEORY.equals(theoryName)) {
			return Optional.empty();
		}
		double defaultPrecision = getPrecisionValue(annotations).orElse(DEFAULT_PRECISION);
		var precisionMap = ObjectDoubleMaps.mutable.<AnyPartialSymbol>empty();
		for (var pair : trace.getInverseRelationTrace().entrySet()) {
			getPrecisionValue(annotationContext.annotationsFor(pair.getValue()))
					.ifPresent(value -> precisionMap.put(pair.getKey(), value));
		}
		double relativeEpsilon = annotations.getAnnotation(IbexAnnotation.IBEX_RELATIVE_EPSILON)
				.flatMap(annotation -> annotation.getBigDecimal(IbexAnnotation.IBEX_RELATIVE_EPSILON_VALUE))
				.map(BigDecimal::doubleValue)
				.orElse(DEFAULT_RELATIVE_EPSILON);
		return Optional.of(new IbexTheory(defaultPrecision, precisionMap.toImmutable(), relativeEpsilon));
	}

	private static Optional<Double> getPrecisionValue(Annotations annotations) {
		return annotations.getAnnotation(IbexAnnotation.IBEX_PRECISION)
				.flatMap(annotation -> annotation.getBigDecimal(IbexAnnotation.IBEX_PRECISION_VALUE))
				.map(BigDecimal::doubleValue);
	}
}
