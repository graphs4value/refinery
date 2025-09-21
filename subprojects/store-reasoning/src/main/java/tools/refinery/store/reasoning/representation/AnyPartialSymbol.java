/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.representation;

import tools.refinery.logic.AnyAbstractDomain;

public sealed interface AnyPartialSymbol permits AnyPartialFunction, PartialSymbol {
	String name();

	int arity();

	AnyAbstractDomain abstractDomain();

	default PartialRelation asPartialRelation() {
		if (this instanceof PartialRelation partialRelation) {
			return partialRelation;
		}
		throw new IllegalStateException("Not a PartialRelation");
	}

	default AnyPartialFunction asPartialFunction() {
		if (this instanceof AnyPartialFunction partialFunction) {
			return partialFunction;
		}
		throw new IllegalStateException("Not a PartialFunction");
	}
}
