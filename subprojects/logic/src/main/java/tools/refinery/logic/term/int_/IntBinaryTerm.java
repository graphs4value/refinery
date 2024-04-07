/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.int_;

import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.Term;

public abstract class IntBinaryTerm extends BinaryTerm<Integer, Integer, Integer> {
	protected IntBinaryTerm(Term<Integer> left, Term<Integer> right) {
		super(Integer.class, Integer.class, Integer.class, left, right);
	}
}
