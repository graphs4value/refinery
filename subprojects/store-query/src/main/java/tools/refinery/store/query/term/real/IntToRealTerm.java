/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.real;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.UnaryTerm;

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
