/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.comparable;

import tools.refinery.logic.term.Term;

public class NotEqTerm<T> extends ComparisonTerm<T> {
	public NotEqTerm(Class<T> argumentType, Term<T> left, Term<T> right) {
		super(argumentType, left, right);
	}

	@Override
	protected Boolean doEvaluate(T leftValue, T rightValue) {
		return !leftValue.equals(rightValue);
	}

	@Override
	public Term<Boolean> withSubTerms(Term<T> newLeft, Term<T> newRight) {
		return new NotEqTerm<>(getArgumentType(), newLeft, newRight);
	}

	@Override
	public String toString() {
		return "(%s != %s)".formatted(getLeft(), getRight());
	}
}
