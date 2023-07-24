/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.bool;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.UnaryTerm;

public class BoolNotTerm extends UnaryTerm<Boolean, Boolean> {
	protected BoolNotTerm(Term<Boolean> body) {
		super(Boolean.class, Boolean.class, body);
	}

	@Override
	protected Term<Boolean> doSubstitute(Substitution substitution, Term<Boolean> substitutedBody) {
		return new BoolNotTerm(substitutedBody);
	}

	@Override
	protected Boolean doEvaluate(Boolean bodyValue) {
		return !bodyValue;
	}

	@Override
	public String toString() {
		return "(!%s)".formatted(getBody());
	}
}
