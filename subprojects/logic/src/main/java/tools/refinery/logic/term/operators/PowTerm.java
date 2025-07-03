/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.operators;

import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.Term;

public class PowTerm<T extends Pow<T>> extends BinaryTerm<T, T, T> {
	public PowTerm(Class<T> type, Term<T> left, Term<T> right) {
		super(type, type, type, left, right);
	}

	@Override
	protected T doEvaluate(T leftValue, T rightValue) {
		return leftValue.pow(rightValue);
	}

	@Override
	protected Term<T> constructWithSubTerms(Term<T> newLeft, Term<T> newRight) {
		return new PowTerm<>(getType(), newLeft, newRight);
	}

	@Override
	public String toString() {
		return "(%s ** %s)".formatted(getLeft(), getRight());
	}
}
