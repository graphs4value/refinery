package tools.refinery.store.query.view;

import tools.refinery.store.model.Model;
import tools.refinery.store.query.FunctionalDependency;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.SymbolLike;

import java.util.Set;

public sealed interface AnyRelationView extends SymbolLike permits RelationView {
	AnySymbol getSymbol();

	default Set<FunctionalDependency<Integer>> getFunctionalDependencies() {
		return Set.of();
	}

	default Set<RelationViewImplication> getImpliedRelationViews() {
		return Set.of();
	}

	boolean get(Model model, Object[] tuple);

	Iterable<Object[]> getAll(Model model);
}
