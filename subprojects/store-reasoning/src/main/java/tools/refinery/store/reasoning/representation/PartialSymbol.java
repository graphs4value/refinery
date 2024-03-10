/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.representation;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.AbstractValue;

public sealed interface PartialSymbol<A extends AbstractValue<A, C>, C> extends AnyPartialSymbol
		permits PartialFunction, PartialRelation {
	@Override
	AbstractDomain<A, C> abstractDomain();

	A defaultValue();

	static PartialRelation of(String name, int arity) {
		return new PartialRelation(name, arity);
	}

	static <A extends AbstractValue<A, C>, C> PartialFunction<A, C> of(
			String name, int arity, AbstractDomain<A, C> abstractDomain) {
		return new PartialFunction<>(name, arity, abstractDomain);
	}
}
