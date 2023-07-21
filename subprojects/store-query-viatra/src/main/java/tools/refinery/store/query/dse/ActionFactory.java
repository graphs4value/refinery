package tools.refinery.store.query.dse;

import org.eclipse.collections.api.block.procedure.Procedure;
import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;

public interface ActionFactory {
	Procedure<Tuple> prepare(Model model);
}
