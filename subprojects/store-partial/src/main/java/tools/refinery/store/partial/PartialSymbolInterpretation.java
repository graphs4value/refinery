package tools.refinery.store.partial;

import tools.refinery.store.partial.representation.PartialSymbol;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.Tuple;

public non-sealed interface PartialSymbolInterpretation<A, C> extends AnyPartialSymbolInterpretation {
	@Override
	PartialSymbol<A, C> getPartialSymbol();

	A get(Tuple key);

	Cursor<Tuple, A> getAll();

	Cursor<Tuple, A> getAllErrors();

	MergeResult merge(Tuple key, A value);

	C getConcrete(Tuple key);

	Cursor<Tuple, C> getAllConcrete();
}
