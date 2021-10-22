package tools.refinery.store.query.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.viatra.query.runtime.api.AdvancedViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.GenericQueryGroup;
import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;
import org.eclipse.viatra.query.runtime.api.IQueryGroup;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelDiffCursor;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.query.QueriableModel;
import tools.refinery.store.query.QueriableModelStore;
import tools.refinery.store.query.RelationalScope;
import tools.refinery.store.query.building.DNFPredicate;

public class QueriableModelImpl implements QueriableModel {
	protected final QueriableModelStore store;
	protected final Model model;

	protected final AdvancedViatraQueryEngine engine;
	protected final Map<DNFPredicate, RawPatternMatcher> predicate2Matcher;

	public QueriableModelImpl(QueriableModelStore store, Model model,
			Map<DNFPredicate, GenericQuerySpecification<RawPatternMatcher>> predicates2PQuery) {
		this.store = store;
		this.model = model;
		this.engine = initEngine(store, model);
		this.predicate2Matcher = initMatchers(engine, predicates2PQuery);
	}

	private AdvancedViatraQueryEngine initEngine(QueriableModelStore store, Model model) {
		RelationalScope scope = new RelationalScope(model, store.getViews());
		return AdvancedViatraQueryEngine.createUnmanagedEngine(scope);
	}

	private Map<DNFPredicate, RawPatternMatcher> initMatchers(AdvancedViatraQueryEngine engine,
			Map<DNFPredicate, GenericQuerySpecification<RawPatternMatcher>> predicates2pQuery) {
		// 1. prepare group
		IQueryGroup queryGroup = GenericQueryGroup.of(Set.copyOf(predicates2pQuery.values()));
		engine.prepareGroup(queryGroup, null);

		// 2. then get all matchers
		Map<DNFPredicate, RawPatternMatcher> result = new HashMap<>();
		for (var entry : predicates2pQuery.entrySet()) {
			var matcher = engine.getMatcher(entry.getValue());
			result.put(entry.getKey(), matcher);
		}
		return result;
	}

	@Override
	public Set<DataRepresentation<?, ?>> getDataRepresentations() {
		return model.getDataRepresentations();
	}

	@Override
	public Set<DNFPredicate> getPredicates() {
		return store.getPredicates();
	}

	@Override
	public <K, V> V get(DataRepresentation<K, V> representation, K key) {
		return model.get(representation, key);
	}

	@Override
	public <K, V> Cursor<K, V> getAll(DataRepresentation<K, V> representation) {
		return model.getAll(representation);
	}

	@Override
	public <K, V> V put(DataRepresentation<K, V> representation, K key, V value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <K, V> void putAll(DataRepresentation<K, V> representation, Cursor<K, V> cursor) {
		// TODO Auto-generated method stub

	}

	@Override
	public <K, V> long getSize(DataRepresentation<K, V> representation) {
		return model.getSize(representation);
	}

	protected PredicateResult getPredicateResult(DNFPredicate predicate) {
		var result = this.predicate2Matcher.get(predicate);
		if (result == null) {
			throw new IllegalArgumentException("Model does not contain predicate " + predicate.getName() + "!");
		} else
			return result;
	}

	protected void validateParameters(DNFPredicate predicate, Object[] parameters) {
		int predicateArity = predicate.getVariables().size();
		int parameterArity = parameters.length;
		if (parameterArity != predicateArity) {
			throw new IllegalArgumentException("Predicate " + predicate.getName() + " with " + predicateArity
					+ " arity called with different number of parameters (" + parameterArity + ")!");
		}
	}

	@Override
	public boolean hasResult(DNFPredicate predicate) {
		return getPredicateResult(predicate).hasResult();
	}

	@Override
	public boolean hasResult(DNFPredicate predicate, Object[] parameters) {
		validateParameters(predicate, parameters);
		return getPredicateResult(predicate).hasResult(parameters);
	}
	
	@Override
	public Optional<Object[]> oneResult(DNFPredicate predicate){
		return getPredicateResult(predicate).oneResult();
	}

	@Override
	public Optional<Object[]> oneResult(DNFPredicate predicate, Object[] parameters){
		validateParameters(predicate, parameters);
		return getPredicateResult(predicate).oneResult(parameters);
	}

	@Override
	public Stream<Object[]> allResults(DNFPredicate predicate){
		return getPredicateResult(predicate).allResults();
	}

	@Override
	public Stream<Object[]> allResults(DNFPredicate predicate, Object[] parameters){
		validateParameters(predicate, parameters);
		return getPredicateResult(predicate).allResults(parameters);
	}

	@Override
	public int countResults(DNFPredicate predicate){
		return getPredicateResult(predicate).countResults();
	}

	@Override
	public int countResults(DNFPredicate predicate, Object[] parameters){
		validateParameters(predicate, parameters);
		return getPredicateResult(predicate).countResults(parameters);
		
	}
	@Override
	public void flushChanges() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ModelDiffCursor getDiffCursor(long to) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long commit() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void restore(long state) {
		// TODO Auto-generated method stub

	}

}
