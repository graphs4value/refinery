/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.bool;

import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.Term;

public abstract class BoolBinaryTerm extends BinaryTerm<Boolean, Boolean, Boolean> {
	protected BoolBinaryTerm(Term<Boolean> left, Term<Boolean> right) {
		super(Boolean.class, Boolean.class, Boolean.class, left, right);
	}
}
