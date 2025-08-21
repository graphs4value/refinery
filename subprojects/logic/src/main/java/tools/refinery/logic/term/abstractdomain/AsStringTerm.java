/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.abstractdomain;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.string.StringValue;

public class AsStringTerm<A extends AbstractValue<A, C>, C> extends AbstractDomainUnaryTerm<StringValue, A, C> {
	AsStringTerm(AbstractDomain<A, C> abstractDomain, Term<A> body) {
		super(StringValue.class, abstractDomain, body);
	}

	@Override
	protected StringValue doEvaluate(A bodyValue) {
		if (bodyValue.isError()) {
			return StringValue.ERROR;
		}
		if (bodyValue.isConcrete()) {
			return StringValue.of(bodyValue.toString());
		}
		return StringValue.UNKNOWN;
	}

	@Override
	protected Term<StringValue> constructWithBody(Term<A> newBody) {
		return new AsStringTerm<>(getAbstractDomain(), newBody);
	}
}
