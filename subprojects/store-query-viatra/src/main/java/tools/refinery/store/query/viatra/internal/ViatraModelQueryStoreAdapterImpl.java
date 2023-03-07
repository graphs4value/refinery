package tools.refinery.store.query.viatra.internal;

import org.eclipse.viatra.query.runtime.api.IQuerySpecification;
import org.eclipse.viatra.query.runtime.api.ViatraQueryEngineOptions;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.dnf.AnyQuery;
import tools.refinery.store.query.viatra.ViatraModelQueryStoreAdapter;
import tools.refinery.store.query.viatra.internal.matcher.RawPatternMatcher;
import tools.refinery.store.query.view.AnyRelationView;

import java.util.*;

public class ViatraModelQueryStoreAdapterImpl implements ViatraModelQueryStoreAdapter {
	private final ModelStore store;
	private final ViatraQueryEngineOptions engineOptions;
	private final Map<AnyRelationView, IInputKey> inputKeys;
	private final Map<AnyQuery, IQuerySpecification<RawPatternMatcher>> querySpecifications;
	private final Set<AnyQuery> vacuousQueries;
	private final Set<AnyQuery> allQueries;

	ViatraModelQueryStoreAdapterImpl(ModelStore store, ViatraQueryEngineOptions engineOptions,
									 Map<AnyRelationView, IInputKey> inputKeys,
									 Map<AnyQuery, IQuerySpecification<RawPatternMatcher>> querySpecifications,
									 Set<AnyQuery> vacuousQueries) {
		this.store = store;
		this.engineOptions = engineOptions;
		this.inputKeys = inputKeys;
		this.querySpecifications = querySpecifications;
		this.vacuousQueries = vacuousQueries;
		var mutableAllQueries = new LinkedHashSet<AnyQuery>(querySpecifications.size() + vacuousQueries.size());
		mutableAllQueries.addAll(querySpecifications.keySet());
		mutableAllQueries.addAll(vacuousQueries);
		this.allQueries = Collections.unmodifiableSet(mutableAllQueries);
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
	public Collection<AnyQuery> getQueries() {
		return allQueries;
	}

	Map<AnyQuery, IQuerySpecification<RawPatternMatcher>> getQuerySpecifications() {
		return querySpecifications;
	}

	Set<AnyQuery> getVacuousQueries() {
		return vacuousQueries;
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
