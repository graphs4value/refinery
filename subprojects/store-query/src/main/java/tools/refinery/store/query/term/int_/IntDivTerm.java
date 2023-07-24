/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.int_;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;

public class IntDivTerm extends IntBinaryTerm {
	public IntDivTerm(Term<Integer> left, Term<Integer> right) {
		super(left, right);
	}

	@Override
	public Term<Integer> doSubstitute(Substitution substitution, Term<Integer> substitutedLeft,
									  Term<Integer> substitutedRight) {
		return new IntDivTerm(substitutedLeft, substitutedRight);
	}

	@Override
	protected Integer doEvaluate(Integer leftValue, Integer rightValue) {
		return rightValue == 0 ? null : leftValue / rightValue;
	}

	@Override
	public String toString() {
		return "(%s / %s)".formatted(getLeft(), getRight());
	}
}
