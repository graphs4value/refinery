package tools.refinery.store.reasoning.representation;

import tools.refinery.store.representation.AnyAbstractDomain;

public sealed interface AnyPartialSymbol permits AnyPartialFunction, PartialSymbol {
	String name();

	int arity();

	AnyAbstractDomain abstractDomain();
}
