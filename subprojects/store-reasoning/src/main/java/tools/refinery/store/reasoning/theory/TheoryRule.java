/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.theory;

import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;

public record TheoryRule(RelationalQuery precondition, Term<TruthValue> assertedTerm,
						 ConcretenessSpecification concretenessSpecification) {
	public TheoryRule(RelationalQuery precondition, Term<TruthValue> assertedTerm) {
		this(precondition, assertedTerm, ConcretenessSpecification.UNSPECIFIED);
	}

	public TheoryRule withConcreteness(ConcretenessSpecification newConcreteness) {
		return new TheoryRule(precondition, assertedTerm, newConcreteness);
	}
}
