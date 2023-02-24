package tools.refinery.store.reasoning;

import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.tuple.Tuple;

public non-sealed interface PartialInterpretation<A, C> extends AnyPartialInterpretation {
	@Override
	PartialSymbol<A, C> getPartialSymbol();

	A get(Tuple key);

	Cursor<Tuple, A> getAll();

	Cursor<Tuple, A> getAllErrors();

	MergeResult merge(Tuple key, A value);

	C getConcrete(Tuple key);

	Cursor<Tuple, C> getAllConcrete();
}
