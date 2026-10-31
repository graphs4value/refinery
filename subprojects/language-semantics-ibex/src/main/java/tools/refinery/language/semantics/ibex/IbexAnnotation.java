/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.ibex;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.annotations.Annotation;
import tools.refinery.language.annotations.DeclarativeAnnotationValidator;
import tools.refinery.language.annotations.ValidateAnnotation;
import tools.refinery.language.model.problem.FunctionDefinition;
import tools.refinery.language.model.problem.ModuleKind;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ReferenceDeclaration;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;

public class IbexAnnotation extends DeclarativeAnnotationValidator {
	public static final QualifiedName IBEX_PRECISION = IbexLibrary.IBEX_LIBRARY.append("ibexPrecision");
	public static final String IBEX_PRECISION_VALUE = "value";
	public static final QualifiedName IBEX_RELATIVE_EPSILON = IbexLibrary.IBEX_LIBRARY.append("ibexRelativeEpsilon");
	public static final String IBEX_RELATIVE_EPSILON_VALUE = "value";

	@Inject
	private ImportAdapterProvider importAdapterProvider;

	@ValidateAnnotation("IBEX_PRECISION")
	private void validatePrecision(Annotation annotation) {
		var target = annotation.getAnnotatedElement();
		if (!isValidPrecisionTarget(target)) {
			error("Annotation %s can only be applied on the top level and on function symbols with data type 'real'."
					.formatted(IBEX_PRECISION), annotation);
		}
		var valueOption = annotation.getBigDecimal(IBEX_PRECISION_VALUE);
		if (valueOption.isEmpty()) {
			error("Must specify %s.".formatted(IBEX_PRECISION_VALUE), annotation);
			return;
		}
		var value = valueOption.get().doubleValue();
		if (value <= 0) {
			error("Precision must be positive.", annotation);
		}
	}

	private boolean isValidPrecisionTarget(EObject target) {
		if (target == null) {
			return false;
		}
		var realDatatype = importAdapterProvider.getBuiltinSymbols(target).realDatatype();
		return switch (target) {
			case Problem problem -> problem.getKind() == ModuleKind.PROBLEM;
			case ReferenceDeclaration referenceDeclaration ->
					realDatatype.equals(referenceDeclaration.getReferenceType());
			case FunctionDefinition functionDefinition -> realDatatype.equals(functionDefinition.getFunctionType());
			default -> false;
		};
	}

	@ValidateAnnotation("IBEX_RELATIVE_EPSILON")
	private void validateRelativeEpsilon(Annotation annotation) {
		if (!(annotation.getAnnotatedElement() instanceof Problem problem) ||
				problem.getKind() != ModuleKind.PROBLEM) {
			error("Annotation %s can only be applied on the top level.".formatted(IBEX_RELATIVE_EPSILON), annotation);
		}
		var valueOption = annotation.getBigDecimal(IBEX_RELATIVE_EPSILON_VALUE);
		if (valueOption.isEmpty()) {
			error("Must specify %s.".formatted(IBEX_RELATIVE_EPSILON_VALUE), annotation);
			return;
		}
		var value = valueOption.get().doubleValue();
		if (value <= 0) {
			error("Relative epsilon must be positive.", annotation);
		}
		if (value >= 1) {
			error("Relative epsilon must be less than 1.", annotation);
		}
	}
}
