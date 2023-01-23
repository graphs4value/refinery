package tools.refinery.store.query.view;

import tools.refinery.store.model.Model;
import tools.refinery.store.representation.SymbolLike;
import tools.refinery.store.representation.AnySymbol;

public sealed interface AnyRelationView extends SymbolLike permits RelationView {
	AnySymbol getSymbol();

	boolean get(Model model, Object[] tuple);

	Iterable<Object[]> getAll(Model model);
}
