/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.truthvalue;

import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;

public class AsTruthValueTerm extends UnaryTerm<TruthValue, Boolean> {
	protected AsTruthValueTerm(Term<Boolean> body) {
		super(TruthValue.class, Boolean.class, body);
	}

	@Override
	protected TruthValue doEvaluate(Boolean bodyValue) {
		return TruthValue.of(bodyValue);
	}

	@Override
	protected Term<TruthValue> constructWithBody(Term<Boolean> newBody) {
		return new AsTruthValueTerm(newBody);
	}

	@Override
	public String toString() {
		return "@Lift(%s)".formatted(getBody());
	}
}
