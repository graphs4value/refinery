/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.int_;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;

public class IntMinTerm extends IntBinaryTerm {
	public IntMinTerm(Term<Integer> left, Term<Integer> right) {
		super(left, right);
	}

	@Override
	public Term<Integer> doSubstitute(Substitution substitution, Term<Integer> substitutedLeft,
									  Term<Integer> substitutedRight) {
		return new IntMinTerm(substitutedLeft, substitutedRight);
	}

	@Override
	protected Integer doEvaluate(Integer leftValue, Integer rightValue) {
		return Math.min(rightValue, leftValue);
	}

	@Override
	public String toString() {
		return "min(%s, %s)".formatted(getLeft(), getRight());
	}
}
