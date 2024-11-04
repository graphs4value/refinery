/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.uppercardinality;

import tools.refinery.logic.term.Term;

public class UpperCardinalityMinTerm extends UpperCardinalityBinaryTerm {
	protected UpperCardinalityMinTerm(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		super(left, right);
	}

	@Override
	protected UpperCardinality doEvaluate(UpperCardinality leftValue, UpperCardinality rightValue) {
		return leftValue.min(rightValue);
	}

	@Override
	public Term<UpperCardinality> withSubTerms(Term<UpperCardinality> newLeft, Term<UpperCardinality> newRight) {
		return new UpperCardinalityMinTerm(newLeft, newRight);
	}

	@Override
	public String toString() {
		return "min(%s, %s)".formatted(getLeft(), getRight());
	}
}
