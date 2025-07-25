/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.logic.term.Term;

public interface PartialTerm<T> extends Term<T> {
	Term<T> orElseConcreteness(ConcretenessSpecification fallback);
}
