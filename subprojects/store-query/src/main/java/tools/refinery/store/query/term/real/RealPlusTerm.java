/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.real;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;

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
