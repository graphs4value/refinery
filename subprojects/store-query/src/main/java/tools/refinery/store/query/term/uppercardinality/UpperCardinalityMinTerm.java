/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.uppercardinality;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.representation.cardinality.UpperCardinality;

public class UpperCardinalityMinTerm extends UpperCardinalityBinaryTerm {
	protected UpperCardinalityMinTerm(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		super(left, right);
	}

	@Override
	protected UpperCardinality doEvaluate(UpperCardinality leftValue, UpperCardinality rightValue) {
		return leftValue.min(rightValue);
	}

	@Override
	public Term<UpperCardinality> doSubstitute(Substitution substitution, Term<UpperCardinality> substitutedLeft, Term<UpperCardinality> substitutedRight) {
		return new UpperCardinalityMinTerm(substitutedLeft, substitutedRight);
	}

	@Override
	public String toString() {
		return "min(%s, %s)".formatted(getLeft(), getRight());
	}
}
