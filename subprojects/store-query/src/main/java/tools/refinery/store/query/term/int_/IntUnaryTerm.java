/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.int_;

import tools.refinery.store.query.term.Term;
import tools.refinery.store.query.term.UnaryTerm;

public abstract class IntUnaryTerm extends UnaryTerm<Integer, Integer> {
	protected IntUnaryTerm(Term<Integer> body) {
		super(Integer.class, Integer.class, body);
	}
}
