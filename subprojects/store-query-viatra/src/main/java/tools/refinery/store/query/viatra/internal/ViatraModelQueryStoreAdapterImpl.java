package tools.refinery.store.query.viatra.internal;

import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngineOptions;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.viatra.ViatraModelQueryStoreAdapter;
import tools.refinery.store.query.viatra.internal.pquery.RawPatternMatcher;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.Collection;
import java.util.Map;

public class ViatraModelQueryStoreAdapterImpl implements ViatraModelQueryStoreAdapter {
	private final ModelStore store;
	private final ViatraQueryEngineOptions engineOptions;
	private final Map<AnyRelationView, IInputKey> inputKeys;
	private final Map<Dnf, IQuerySpecification<RawPatternMatcher>> querySpecifications;

	ViatraModelQueryStoreAdapterImpl(ModelStore store, ViatraQueryEngineOptions engineOptions,
									 Map<AnyRelationView, IInputKey> inputKeys,
									 Map<Dnf, IQuerySpecification<RawPatternMatcher>> querySpecifications) {
		this.store = store;
		this.engineOptions = engineOptions;
		this.inputKeys = inputKeys;
		this.querySpecifications = querySpecifications;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	public Collection<AnyRelationView> getRelationViews() {
		return inputKeys.keySet();
	}

	public Map<AnyRelationView, IInputKey> getInputKeys() {
		return inputKeys;
	}

	@Override
	public Collection<Dnf> getQueries() {
		return querySpecifications.keySet();
	}

	Map<Dnf, IQuerySpecification<RawPatternMatcher>> getQuerySpecifications() {
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
