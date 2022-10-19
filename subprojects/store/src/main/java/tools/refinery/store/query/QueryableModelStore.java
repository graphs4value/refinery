package tools.refinery.store.query;

import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.query.view.RelationView;

import java.util.Set;

public interface QueryableModelStore extends ModelStore {
	@SuppressWarnings("squid:S1452")
	Set<DataRepresentation<?, ?>> getDataRepresentations();

	@SuppressWarnings("squid:S1452")
	Set<RelationView<?>> getViews();

	Set<DNF> getPredicates();

	QueryableModel createModel();

	QueryableModel createModel(long state);
}
