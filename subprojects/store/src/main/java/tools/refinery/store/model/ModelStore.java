package tools.refinery.store.model;

import tools.refinery.store.model.representation.AnyDataRepresentation;

import java.util.Set;

public interface ModelStore {
	Set<AnyDataRepresentation> getDataRepresentations();

	Model createModel();

	Model createModel(long state);

	Set<Long> getStates();

	ModelDiffCursor getDiffCursor(long from, long to);
}
