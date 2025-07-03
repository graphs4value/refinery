/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.abstractdomain;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.Term;

public class AbstractDomainMeetTerm<A extends AbstractValue<A, C>, C>
		extends AbstractDomainBinaryTerm<A, A, C> {
	public AbstractDomainMeetTerm(AbstractDomain<A, C> abstractDomain, Term<A> left, Term<A> right) {
		super(abstractDomain.abstractType(), abstractDomain, left, right);
	}

	@Override
	protected A doEvaluate(A leftValue, A rightValue) {
		return leftValue.meet(rightValue);
	}

	@Override
	protected Term<A> constructWithSubTerms(Term<A> newLeft, Term<A> newRight) {
		return new AbstractDomainMeetTerm<>(getAbstractDomain(), newLeft, newRight);
	}

	@Override
	public String toString() {
		return "(%s /\\ %s)".formatted(getLeft(), getRight());
	}
}
