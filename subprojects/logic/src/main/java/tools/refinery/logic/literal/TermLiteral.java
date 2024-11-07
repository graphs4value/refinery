/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.literal;

import tools.refinery.logic.term.Term;

public interface TermLiteral<T> extends Literal {
	Term<T> getTerm();

	TermLiteral<T> withTerm(Term<T> term);
}
