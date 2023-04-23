/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.uppercardinality;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.representation.cardinality.UpperCardinality;

public class UpperCardinalityAddTerm extends UpperCardinalityBinaryTerm {
	protected UpperCardinalityAddTerm(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		super(left, right);
	}

	@Override
	protected UpperCardinality doEvaluate(UpperCardinality leftValue, UpperCardinality rightValue) {
		return leftValue.add(rightValue);
	}

	@Override
	public Term<UpperCardinality> doSubstitute(Substitution substitution, Term<UpperCardinality> substitutedLeft, Term<UpperCardinality> substitutedRight) {
		return new UpperCardinalityAddTerm(substitutedLeft, substitutedRight);
	}

	@Override
	public String toString() {
		return "(%s + %s)".formatted(getLeft(), getRight());
	}
}
