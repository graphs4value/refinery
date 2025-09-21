/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.operators;

import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;

public class NotTerm<T extends Not<T>> extends UnaryTerm<T, T> {
	public NotTerm(Class<T> type, Term<T> body) {
		super(type, type, body);
	}

	@Override
	protected T doEvaluate(T bodyValue) {
		return bodyValue.not();
	}

	@Override
	protected Term<T> constructWithBody(Term<T> newBody) {
		return new NotTerm<>(getType(), newBody);
	}

	@Override
	public String toString() {
		return "(!%s)".formatted(getBody());
	}
}
