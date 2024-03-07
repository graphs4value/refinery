/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.int_;

import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;
import tools.refinery.logic.substitution.Substitution;

public class RealToIntTerm extends UnaryTerm<Integer, Double> {
	protected RealToIntTerm(Term<Double> body) {
		super(Integer.class, Double.class, body);
	}

	@Override
	protected Integer doEvaluate(Double bodyValue) {
		return bodyValue.isNaN() ? null : bodyValue.intValue();
	}

	@Override
	protected Term<Integer> doSubstitute(Substitution substitution, Term<Double> substitutedBody) {
		return new RealToIntTerm(substitutedBody);
	}

	@Override
	public String toString() {
		return "(%s as int)".formatted(getBody());
	}
}
