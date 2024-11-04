/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.rewriter;

import tools.refinery.logic.term.AnyTerm;
import tools.refinery.logic.term.Term;

@FunctionalInterface
public interface TermRewriter {
	<T> Term<T> rewriteTerm(Term<T> term);

	default AnyTerm rewriteTerm(AnyTerm term) {
		return rewriteTerm((Term<?>) term);
	}
}
