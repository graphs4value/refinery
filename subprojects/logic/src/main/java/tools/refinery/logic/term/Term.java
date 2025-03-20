/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import tools.refinery.logic.rewriter.TermRewriter;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.valuation.Valuation;

public non-sealed interface Term<T> extends AnyTerm {
	@Override
	Class<T> getType();

	T evaluate(Valuation valuation);

	@Override
	Term<T> rewriteSubTerms(TermRewriter termRewriter);

	@Override
	Term<T> substitute(Substitution substitution);
}
