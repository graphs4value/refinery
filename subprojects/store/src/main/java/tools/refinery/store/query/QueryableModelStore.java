package tools.refinery.store.query;

import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.representation.AnyDataRepresentation;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.Set;

public interface QueryableModelStore extends ModelStore {
    Set<AnyDataRepresentation> getDataRepresentations();

	Set<AnyRelationView> getViews();

	Set<DNF> getPredicates();

	QueryableModel createModel();

	QueryableModel createModel(long state);
}
