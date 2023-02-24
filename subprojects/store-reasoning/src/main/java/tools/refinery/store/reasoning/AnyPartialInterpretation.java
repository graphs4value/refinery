package tools.refinery.store.reasoning;

import tools.refinery.store.reasoning.representation.AnyPartialSymbol;

public sealed interface AnyPartialInterpretation permits PartialInterpretation {
	ReasoningAdapter getAdapter();

	AnyPartialSymbol getPartialSymbol();

	int countUnfinished();

	int countErrors();
}
