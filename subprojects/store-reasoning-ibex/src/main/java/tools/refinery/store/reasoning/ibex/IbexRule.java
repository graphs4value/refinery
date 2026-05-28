/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.ibex;

import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;

public record IbexRule(RelationalQuery precondition, Term<TruthValue> assertedTerm) {
}
