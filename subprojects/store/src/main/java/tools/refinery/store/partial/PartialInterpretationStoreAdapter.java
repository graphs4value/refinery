package tools.refinery.store.partial;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.model.Model;

public interface PartialInterpretationStoreAdapter extends ModelStoreAdapter {
	@Override
	PartialInterpretationAdapter createModelAdapter(Model model);
}
