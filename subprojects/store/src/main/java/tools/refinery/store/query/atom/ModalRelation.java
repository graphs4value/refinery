package tools.refinery.store.query.atom;

import tools.refinery.store.representation.SymbolLike;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;

public record ModalRelation(Modality modality, Symbol<TruthValue> relation) implements SymbolLike {
	@Override
	public String name() {
		return "%s %s".formatted(modality, relation);
	}

	@Override
	public int arity() {
		return relation.arity();
	}
}
