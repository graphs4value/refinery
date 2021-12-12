package tools.refinery.store.query;

import java.util.Set;

import tools.refinery.store.model.ModelDiffCursor;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.query.building.DNFPredicate;
import tools.refinery.store.query.view.RelationView;

public interface QueriableModelStore extends ModelStore{
	@SuppressWarnings("squid:S1452")
	Set<DataRepresentation<?, ?>> getDataRepresentations();
	@SuppressWarnings("squid:S1452")
	Set<RelationView<?>> getViews();
	Set<DNFPredicate> getPredicates();
	
	QueriableModel createModel();
	QueriableModel createModel(long state);
	
	Set<Long> getStates();
	ModelDiffCursor getDiffCursor(long from, long to);
}
