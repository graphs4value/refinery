/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.bool;

import tools.refinery.logic.term.Term;

public class BoolOrTerm extends BoolBinaryTerm {
	public BoolOrTerm(Term<Boolean> left, Term<Boolean> right) {
		super(left, right);
	}

	@Override
    protected Term<Boolean> constructWithSubTerms(Term<Boolean> newLeft,
                                                  Term<Boolean> newRight) {
		return new BoolOrTerm(newLeft, newRight);
	}

	@Override
	protected Boolean doEvaluate(Boolean leftValue, Boolean rightValue) {
		return leftValue || rightValue;
	}

	@Override
	public String toString() {
		return "(%s || %s)".formatted(getLeft(), getRight());
	}
}
