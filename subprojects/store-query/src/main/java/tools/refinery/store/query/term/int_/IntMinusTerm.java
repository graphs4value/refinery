/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.int_;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;

public class IntMinusTerm extends IntUnaryTerm {
	public IntMinusTerm(Term<Integer> body) {
		super(body);
	}

	@Override
	protected Term<Integer> doSubstitute(Substitution substitution, Term<Integer> substitutedBody) {
		return new IntMinusTerm(substitutedBody);
	}

	@Override
	protected Integer doEvaluate(Integer bodyValue) {
		return -bodyValue;
	}

	@Override
	public String toString() {
		return "(-%s)".formatted(getBody());
	}
}
