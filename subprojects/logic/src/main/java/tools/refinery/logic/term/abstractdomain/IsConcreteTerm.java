/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.abstractdomain;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.Term;

public class IsConcreteTerm<A extends AbstractValue<A, C>, C> extends AbstractDomainUnaryTerm<Boolean, A, C> {
	protected IsConcreteTerm(AbstractDomain<A, C> abstractDomain, Term<A> body) {
		super(Boolean.class, abstractDomain, body);
	}

	@Override
	protected Boolean doEvaluate(A bodyValue) {
		return bodyValue.isConcrete();
	}

	@Override
	protected Term<Boolean> constructWithBody(Term<A> newBody) {
		return new IsConcreteTerm<>(getAbstractDomain(), newBody);
	}

	@Override
	public String toString() {
		return "isConcrete(%s)".formatted(super.toString());
	}
}
