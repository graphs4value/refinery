/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.operators;

import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;

public class ExpTerm<T extends Exp<T>> extends UnaryTerm<T, T> {
	public ExpTerm(Class<T> type, Term<T> body) {
		super(type, type, body);
	}

	@Override
	protected T doEvaluate(T bodyValue) {
		return bodyValue.exp();
	}

	@Override
	protected Term<T> constructWithBody(Term<T> newBody) {
		return new ExpTerm<>(getType(), newBody);
	}

	@Override
	public String toString() {
		return "exp(%s)".formatted(getBody());
	}
}
