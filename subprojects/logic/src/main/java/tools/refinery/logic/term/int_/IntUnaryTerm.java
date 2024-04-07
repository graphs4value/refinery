/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.int_;

import tools.refinery.logic.term.UnaryTerm;
import tools.refinery.logic.term.Term;

public abstract class IntUnaryTerm extends UnaryTerm<Integer, Integer> {
	protected IntUnaryTerm(Term<Integer> body) {
		super(Integer.class, Integer.class, body);
	}
}
