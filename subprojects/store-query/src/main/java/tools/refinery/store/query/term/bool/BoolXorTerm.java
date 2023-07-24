/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.bool;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;

public class BoolXorTerm extends BoolBinaryTerm {
	public BoolXorTerm(Term<Boolean> left, Term<Boolean> right) {
		super(left, right);
	}

	@Override
	public Term<Boolean> doSubstitute(Substitution substitution, Term<Boolean> substitutedLeft,
									  Term<Boolean> substitutedRight) {
		return new BoolXorTerm(substitutedLeft, substitutedRight);
	}

	@Override
	protected Boolean doEvaluate(Boolean leftValue, Boolean rightValue) {
		return leftValue ^ rightValue;
	}

	@Override
	public String toString() {
		return "(%s ^^ %s)".formatted(getLeft(), getRight());
	}
}
