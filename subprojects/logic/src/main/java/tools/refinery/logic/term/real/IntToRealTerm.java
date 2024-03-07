/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.real;

import tools.refinery.logic.term.UnaryTerm;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.Term;

public class IntToRealTerm extends UnaryTerm<Double, Integer> {
	protected IntToRealTerm(Term<Integer> body) {
		super(Double.class, Integer.class, body);
	}

	@Override
	protected Term<Double> doSubstitute(Substitution substitution, Term<Integer> substitutedBody) {
		return new IntToRealTerm(substitutedBody);
	}

	@Override
	protected Double doEvaluate(Integer bodyValue) {
		return bodyValue.doubleValue();
	}

	@Override
	public String toString() {
		return "(%s as real)".formatted(getBody());
	}
}
