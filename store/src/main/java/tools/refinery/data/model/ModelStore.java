package tools.refinery.data.model;

import java.util.Set;

import tools.refinery.data.model.representation.DataRepresentation;

public interface ModelStore {
	@SuppressWarnings("squid:S1452")
	Set<DataRepresentation<?, ?>> getDataRepresentations();
	
	Model createModel();
	Model createModel(long state);
	
	Set<Long> getStates();
	ModelDiffCursor getDiffCursor(long from, long to);
}