package tools.refinery.store.reasoning;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.query.dnf.Dnf;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnusedReturnValue")
public interface ReasoningBuilder extends ModelAdapterBuilder {
	default ReasoningBuilder liftedQueries(Dnf... liftedQueries) {
		return liftedQueries(List.of(liftedQueries));
	}

	default ReasoningBuilder liftedQueries(Collection<Dnf> liftedQueries) {
		liftedQueries.forEach(this::liftedQuery);
		return this;
	}

	ReasoningBuilder liftedQuery(Dnf liftedQuery);

	Dnf lift(Modality modality, Dnf query);

	@Override
	ReasoningStoreAdapter createStoreAdapter(ModelStore store);
}
