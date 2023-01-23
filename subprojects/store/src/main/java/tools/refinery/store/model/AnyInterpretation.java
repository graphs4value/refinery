package tools.refinery.store.model;

import tools.refinery.store.representation.AnySymbol;

public sealed interface AnyInterpretation permits Interpretation {
	Model getModel();

	AnySymbol getSymbol();

	long getSize();
}
