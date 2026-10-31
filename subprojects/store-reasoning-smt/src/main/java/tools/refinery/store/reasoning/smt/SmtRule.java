/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt;

import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;

public record SmtRule(RelationalQuery precondition, Term<TruthValue> assertedTerm,
					  ConcretenessSpecification concreteness) {
	public SmtRule(RelationalQuery precondition, Term<TruthValue> assertedTerm) {
		this(precondition, assertedTerm, ConcretenessSpecification.UNSPECIFIED);
	}

	public SmtRule withConcreteness(ConcretenessSpecification newConcreteness) {
		return new SmtRule(precondition, assertedTerm, newConcreteness);
	}
}
