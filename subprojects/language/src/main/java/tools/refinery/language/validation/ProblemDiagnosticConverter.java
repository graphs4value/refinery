/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.validation;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.validation.DiagnosticConverterImpl;
import tools.refinery.language.model.problem.Multiplicity;
import tools.refinery.language.model.problem.ReferenceDeclaration;
import tools.refinery.language.services.ProblemGrammarAccess;

public class ProblemDiagnosticConverter extends DiagnosticConverterImpl {
	@Inject
	private ProblemGrammarAccess grammarAccess;

	@Override
	protected IssueLocation getLocationData(EObject obj, EStructuralFeature structuralFeature, int index) {
		if (structuralFeature == null && obj instanceof Multiplicity &&
				obj.eContainer() instanceof ReferenceDeclaration referenceDeclaration) {
			// Include the enclosing {@code []} square braces in the error location.
			// This lets use have a non-0 length error marker for invalid container references such as
			// {@code container Foo[] foo opposite bar}, where unbounded multiplicities are disallowed.
			return getLocationData(referenceDeclaration, obj.eContainingFeature());
		}
		return super.getLocationData(obj, structuralFeature, index);
	}
}
