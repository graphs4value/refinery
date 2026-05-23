/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.ibex;

import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;

/**
 * A single IBEX propagation rule: whenever {@code precondition} holds for a tuple of nodes,
 * the {@code assertedTerm} (an arithmetic comparison) is enforced by interval contraction.
 * <p>
 * Unlike {@code SmtRule}, there is no concreteness specification: IBEX rules only participate
 * in the propagation stage (PARTIAL concreteness). They do not concretize values.
 */
public record IbexRule(RelationalQuery precondition, Term<TruthValue> assertedTerm) {
}