/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.abstractdomain;

import tools.refinery.logic.ComparableAbstractDomain;
import tools.refinery.logic.term.ComparableAbstractValue;
import tools.refinery.logic.term.Term;

public class AbstractDomainUpperBoundTerm<A extends ComparableAbstractValue<A, C>, C extends Comparable<C>>
		extends AbstractDomainUnaryTerm<A, A, C> {
	protected AbstractDomainUpperBoundTerm(ComparableAbstractDomain<A, C> abstractDomain, Term<A> body) {
		super(abstractDomain.abstractType(), abstractDomain, body);
	}

	@Override
	protected A doEvaluate(A bodyValue) {
		return bodyValue.abstractUpperBound();
	}

	@Override
	protected Term<A> constructWithBody(Term<A> newBody) {
		return new AbstractDomainUpperBoundTerm<>((ComparableAbstractDomain<A, C>) getAbstractDomain(), newBody);
	}

	@Override
	public String toString() {
		return "upperBound(%s)".formatted(super.toString());
	}
}
