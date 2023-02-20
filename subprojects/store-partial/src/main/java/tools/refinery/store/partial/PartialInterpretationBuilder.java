package tools.refinery.store.partial;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.Dnf;
import tools.refinery.store.partial.literal.Modality;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnusedReturnValue")
public interface PartialInterpretationBuilder extends ModelAdapterBuilder {
	default PartialInterpretationBuilder liftedQueries(Dnf... liftedQueries) {
		return liftedQueries(List.of(liftedQueries));
	}

	default PartialInterpretationBuilder liftedQueries(Collection<Dnf> liftedQueries) {
		liftedQueries.forEach(this::liftedQuery);
		return this;
	}

	PartialInterpretationBuilder liftedQuery(Dnf liftedQuery);

	Dnf lift(Modality modality, Dnf query);

	@Override
	PartialInterpretationStoreAdapter createStoreAdapter(ModelStore store);
}
