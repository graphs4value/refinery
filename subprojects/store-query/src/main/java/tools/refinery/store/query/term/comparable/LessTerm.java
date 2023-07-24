/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.comparable;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;

public class LessTerm<T extends Comparable<T>> extends ComparisonTerm<T> {
	public LessTerm(Class<T> argumentType, Term<T> left, Term<T> right) {
		super(argumentType, left, right);
	}

	@Override
	protected Boolean doEvaluate(T leftValue, T rightValue) {
		return leftValue.compareTo(rightValue) < 0;
	}

	@Override
	public Term<Boolean> doSubstitute(Substitution substitution, Term<T> substitutedLeft, Term<T> substitutedRight) {
		return new LessTerm<>(getArgumentType(), substitutedLeft, substitutedRight);
	}

	@Override
	public String toString() {
		return "(%s < %s)".formatted(getLeft(), getRight());
	}
}
