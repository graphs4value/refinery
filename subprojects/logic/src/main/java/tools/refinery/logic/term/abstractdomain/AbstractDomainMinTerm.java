/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.abstractdomain;

import tools.refinery.logic.ComparableAbstractDomain;
import tools.refinery.logic.term.ComparableAbstractValue;
import tools.refinery.logic.term.Term;

public class AbstractDomainMinTerm<A extends ComparableAbstractValue<A, C>, C extends Comparable<C>>
		extends AbstractDomainBinaryTerm<A, A, C> {
	public AbstractDomainMinTerm(ComparableAbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		super(abstractDomain.abstractType(), abstractDomain, left, right);
	}

	@Override
	protected A doEvaluate(A leftValue, A rightValue) {
		return leftValue.min(rightValue);
	}

	@Override
	protected Term<A> constructWithSubTerms(Term<A> newLeft, Term<A> newRight) {
		return new AbstractDomainMinTerm<>((ComparableAbstractDomain<A, C>) getAbstractDomain(), newLeft, newRight);
	}

	@Override
	public String toString() {
		return "min(%s, %s)".formatted(getLeft(), getRight());
	}
}
