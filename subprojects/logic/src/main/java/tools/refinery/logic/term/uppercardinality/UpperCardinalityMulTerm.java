/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.uppercardinality;

import tools.refinery.logic.term.Term;

public class UpperCardinalityMulTerm extends UpperCardinalityBinaryTerm {
	protected UpperCardinalityMulTerm(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		super(left, right);
	}

	@Override
	protected UpperCardinality doEvaluate(UpperCardinality leftValue, UpperCardinality rightValue) {
		return leftValue.multiply(rightValue);
	}

	@Override
    protected Term<UpperCardinality> constructWithSubTerms(Term<UpperCardinality> newLeft, Term<UpperCardinality> newRight) {
		return new UpperCardinalityMulTerm(newLeft, newRight);
	}

	@Override
	public String toString() {
		return "(%s * %s)".formatted(getLeft(), getRight());
	}
}
