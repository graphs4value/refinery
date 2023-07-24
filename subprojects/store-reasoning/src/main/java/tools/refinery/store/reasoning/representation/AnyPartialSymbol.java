/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.representation;

import tools.refinery.store.representation.AnyAbstractDomain;

public sealed interface AnyPartialSymbol permits AnyPartialFunction, PartialSymbol {
	String name();

	int arity();

	AnyAbstractDomain abstractDomain();
}
