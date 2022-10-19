package tools.refinery.store.query.viatra.internal;

import org.eclipse.viatra.query.runtime.api.AdvancedViatraQueryEngine;
import org.eclipse.viatra.query.runtime.api.GenericQueryGroup;
import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;
import org.eclipse.viatra.query.runtime.api.IQueryGroup;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.map.DiffCursor;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelDiffCursor;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.query.QueryableModel;
import tools.refinery.store.query.QueryableModelStore;
import tools.refinery.store.query.DNF;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.TupleLike;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class ViatraQueryableModel implements QueryableModel {
	protected final QueryableModelStore store;

	protected final Model model;

	protected final Map<DNF, GenericQuerySpecification<RawPatternMatcher>> predicates2PQuery;

	protected RelationalScope scope;

	protected AdvancedViatraQueryEngine engine;

	protected Map<DNF, RawPatternMatcher> predicate2Matcher;

	public ViatraQueryableModel(QueryableModelStore store, Model model,
								Map<DNF, GenericQuerySpecification<RawPatternMatcher>> predicates2PQuery) {
		this.store = store;
		this.model = model;
		this.predicates2PQuery = predicates2PQuery;
		initEngine();
	}

	private void initEngine() {
		this.scope = new RelationalScope(this.model, this.store.getViews());
		this.engine = AdvancedViatraQueryEngine.createUnmanagedEngine(this.scope);
		this.predicate2Matcher = initMatchers(this.engine, this.predicates2PQuery);
	}

	private Map<DNF, RawPatternMatcher> initMatchers(
			AdvancedViatraQueryEngine engine,
			Map<DNF, GenericQuerySpecification<RawPatternMatcher>> predicates2pQuery) {
		// 1. prepare group
		IQueryGroup queryGroup = GenericQueryGroup.of(Set.copyOf(predicates2pQuery.values()));
		engine.prepareGroup(queryGroup, null);

		// 2. then get all matchers
		Map<DNF, RawPatternMatcher> result = new HashMap<>();
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
	public Set<DNF> getPredicates() {
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

	@SuppressWarnings("unchecked")
	@Override
	public <K, V> V put(DataRepresentation<K, V> representation, K key, V value) {
		V oldValue = this.model.put(representation, key, value);
		if (representation instanceof Relation<?> relation) {
			this.scope.processUpdate((Relation<V>) relation, (Tuple) key, oldValue, value);
		}
		return oldValue;
	}

	@Override
	public <K, V> void putAll(DataRepresentation<K, V> representation, Cursor<K, V> cursor) {
		if (representation instanceof Relation<?>) {
			//noinspection RedundantSuppression
			@SuppressWarnings("unchecked")
			Relation<V> relation = (Relation<V>) representation;
			while (cursor.move()) {
				Tuple key = (Tuple) cursor.getKey();
				V newValue = cursor.getValue();
				V oldValue = this.model.put(relation, key, newValue);
				this.scope.processUpdate(relation, key, oldValue, newValue);
			}
		} else {
			this.model.putAll(representation, cursor);
		}
	}

	@Override
	public <K, V> long getSize(DataRepresentation<K, V> representation) {
		return model.getSize(representation);
	}

	protected RawPatternMatcher getMatcher(DNF predicate) {
		var result = this.predicate2Matcher.get(predicate);
		if (result == null) {
			throw new IllegalArgumentException("Model does not contain predicate %s".formatted(predicate.getName()));
		} else
			return result;
	}

	protected void validateParameters(DNF predicate, Tuple parameters) {
		int predicateArity = predicate.getParameters().size();
		int parameterArity = parameters.getSize();
		if (parameterArity != predicateArity) {
			throw new IllegalArgumentException(
					"Predicate %s with %d arity called with different number of parameters (%d)"
							.formatted(predicate.getName(), predicateArity, parameterArity));
		}
	}

	@Override
	public boolean hasResult(DNF predicate) {
		return getMatcher(predicate).hasResult();
	}

	@Override
	public boolean hasResult(DNF predicate, Tuple parameters) {
		validateParameters(predicate, parameters);
		return getMatcher(predicate).hasResult(parameters);
	}

	@Override
	public Optional<TupleLike> oneResult(DNF predicate) {
		return getMatcher(predicate).oneResult();
	}

	@Override
	public Optional<TupleLike> oneResult(DNF predicate, Tuple parameters) {
		validateParameters(predicate, parameters);
		return getMatcher(predicate).oneResult(parameters);
	}

	@Override
	public Stream<TupleLike> allResults(DNF predicate) {
		return getMatcher(predicate).allResults();
	}

	@Override
	public Stream<TupleLike> allResults(DNF predicate, Tuple parameters) {
		validateParameters(predicate, parameters);
		return getMatcher(predicate).allResults(parameters);
	}

	@Override
	public int countResults(DNF predicate) {
		return getMatcher(predicate).countResults();
	}

	@Override
	public int countResults(DNF predicate, Tuple parameters) {
		validateParameters(predicate, parameters);
		return getMatcher(predicate).countResults(parameters);

	}

	@Override
	public boolean hasChanges() {
		return scope.hasChanges();
	}

	@Override
	public void flushChanges() {
		this.scope.flush();
	}

	@Override
	public ModelDiffCursor getDiffCursor(long to) {
		return model.getDiffCursor(to);
	}

	@Override
	public long commit() {
		return this.model.commit();
	}

	@Override
	public void restore(long state) {
		restoreWithDiffReplay(state);
	}

	private void restoreWithDiffReplay(long state) {
		var modelDiffCursor = getDiffCursor(state);
		for (DataRepresentation<?, ?> dataRepresentation : this.getDataRepresentations()) {
			restoreRepresentationWithDiffReplay(modelDiffCursor, dataRepresentation);
		}
	}

	private <K, V> void restoreRepresentationWithDiffReplay(ModelDiffCursor modelDiffCursor,
															DataRepresentation<K, V> dataRepresentation) {
		DiffCursor<K, V> diffCursor = modelDiffCursor.getCursor(dataRepresentation);
		this.putAll(dataRepresentation, diffCursor);
	}
}
