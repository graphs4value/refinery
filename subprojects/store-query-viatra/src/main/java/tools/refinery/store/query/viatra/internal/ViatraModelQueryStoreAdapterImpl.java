package tools.refinery.store.query.viatra.internal;

import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngineOptions;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.DNF;
import tools.refinery.store.query.viatra.ViatraModelQueryStoreAdapter;
import tools.refinery.store.query.viatra.internal.pquery.RawPatternMatcher;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.Collection;
import java.util.Map;

public class ViatraModelQueryStoreAdapterImpl implements ViatraModelQueryStoreAdapter {
	private final ModelStore store;
	private final ViatraQueryEngineOptions engineOptions;
	private final Collection<AnyRelationView> relationViews;
	private final Map<DNF, IQuerySpecification<RawPatternMatcher>> querySpecifications;

	ViatraModelQueryStoreAdapterImpl(ModelStore store, ViatraQueryEngineOptions engineOptions,
									 Collection<AnyRelationView> relationViews,
									 Map<DNF, IQuerySpecification<RawPatternMatcher>> querySpecifications) {
		this.store = store;
		this.engineOptions = engineOptions;
		this.relationViews = relationViews;
		this.querySpecifications = querySpecifications;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public Collection<AnyRelationView> getRelationViews() {
		return relationViews;
	}

	@Override
	public Collection<DNF> getQueries() {
		return querySpecifications.keySet();
	}

	Map<DNF, IQuerySpecification<RawPatternMatcher>> getQuerySpecifications() {
		return querySpecifications;
	}

	@Override
	public ViatraQueryEngineOptions getEngineOptions() {
		return engineOptions;
	}

	@Override
	public ViatraModelQueryAdapterImpl createModelAdapter(Model model) {
		return new ViatraModelQueryAdapterImpl(model, this);
	}
}
