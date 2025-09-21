/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.operators;

import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.Term;

public class OrTerm<T extends Or<T>> extends BinaryTerm<T, T, T> {
	public OrTerm(Class<T> type, Term<T> left, Term<T> right) {
		super(type, type, type, left, right);
	}

	@Override
	protected T doEvaluate(T leftValue, T rightValue) {
		return leftValue.or(rightValue);
	}

	@Override
	protected Term<T> constructWithSubTerms(Term<T> newLeft, Term<T> newRight) {
		return new OrTerm<>(getType(), newLeft, newRight);
	}

	@Override
	public String toString() {
		return "(%s || %s)".formatted(getLeft(), getRight());
	}
}
