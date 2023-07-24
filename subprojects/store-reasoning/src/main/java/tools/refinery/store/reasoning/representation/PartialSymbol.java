/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.representation;

import tools.refinery.store.representation.AbstractDomain;

public sealed interface PartialSymbol<A, C> extends AnyPartialSymbol permits PartialFunction, PartialRelation {
	@Override
	AbstractDomain<A, C> abstractDomain();

	A defaultValue();

	C defaultConcreteValue();
}
