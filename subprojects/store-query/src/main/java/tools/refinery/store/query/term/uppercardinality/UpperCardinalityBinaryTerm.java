/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term.uppercardinality;

import tools.refinery.store.query.term.BinaryTerm;
import tools.refinery.store.query.term.Term;
import tools.refinery.store.representation.cardinality.UpperCardinality;

public abstract class UpperCardinalityBinaryTerm extends BinaryTerm<UpperCardinality, UpperCardinality,
		UpperCardinality> {
	protected UpperCardinalityBinaryTerm(Term<UpperCardinality> left, Term<UpperCardinality> right) {
		super(UpperCardinality.class, UpperCardinality.class, UpperCardinality.class, left, right);
	}
}
