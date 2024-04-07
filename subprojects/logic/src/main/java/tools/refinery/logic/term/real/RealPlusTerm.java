/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.real;

import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.Term;

public class RealPlusTerm extends RealUnaryTerm {
	public RealPlusTerm(Term<Double> body) {
		super(body);
	}

	@Override
	protected Term<Double> doSubstitute(Substitution substitution, Term<Double> substitutedBody) {
		return new RealPlusTerm(substitutedBody);
	}

	@Override
	protected Double doEvaluate(Double bodyValue) {
		return bodyValue;
	}

	@Override
	public String toString() {
		return "(+%s)".formatted(getBody());
	}
}
