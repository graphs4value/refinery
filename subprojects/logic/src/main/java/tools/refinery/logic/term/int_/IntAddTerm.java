/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.int_;

import tools.refinery.logic.term.Term;

public class IntAddTerm extends IntBinaryTerm {
	public IntAddTerm(Term<Integer> left, Term<Integer> right) {
		super(left, right);
	}

	@Override
    protected Term<Integer> constructWithSubTerms(Term<Integer> newLeft,
                                                  Term<Integer> newRight) {
		return new IntAddTerm(newLeft, newRight);
	}

	@Override
	protected Integer doEvaluate(Integer leftValue, Integer rightValue) {
		return leftValue + rightValue;
	}

	@Override
	public String toString() {
		return "(%s + %s)".formatted(getLeft(), getRight());
	}
}
