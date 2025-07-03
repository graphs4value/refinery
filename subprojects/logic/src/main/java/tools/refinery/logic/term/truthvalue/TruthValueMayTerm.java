/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.truthvalue;

import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;

public class TruthValueMayTerm extends UnaryTerm<Boolean, TruthValue> {
	protected TruthValueMayTerm(Term<TruthValue> body) {
		super(Boolean.class, TruthValue.class, body);
	}

	@Override
	protected Boolean doEvaluate(TruthValue bodyValue) {
		return bodyValue.may();
	}

	@Override
	protected Term<Boolean> constructWithBody(Term<TruthValue> newBody) {
		return new TruthValueMayTerm(newBody);
	}

	@Override
	public String toString() {
		return "(may %s)".formatted(getBody());
	}
}
