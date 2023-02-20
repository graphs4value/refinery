package tools.refinery.store.partial;

import tools.refinery.store.partial.representation.AnyPartialSymbol;

public sealed interface AnyPartialSymbolInterpretation permits PartialSymbolInterpretation {
	PartialInterpretationAdapter getAdapter();

	AnyPartialSymbol getPartialSymbol();

	int countUnfinished();

	int countErrors();
}
