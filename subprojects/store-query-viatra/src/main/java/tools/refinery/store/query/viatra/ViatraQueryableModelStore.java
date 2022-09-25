package tools.refinery.store.query.viatra;

import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;
import tools.refinery.store.model.ModelDiffCursor;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreImpl;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.query.QueryableModel;
import tools.refinery.store.query.QueryableModelStore;
import tools.refinery.store.query.building.*;
import tools.refinery.store.query.viatra.internal.ViatraQueryableModel;
import tools.refinery.store.query.viatra.internal.pquery.DNF2PQuery;
import tools.refinery.store.query.viatra.internal.RawPatternMatcher;
import tools.refinery.store.query.viatra.internal.pquery.SimplePQuery;
import tools.refinery.store.query.view.RelationView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ViatraQueryableModelStore implements QueryableModelStore {
	protected final ModelStore store;
	protected final Set<RelationView<?>> relationViews;
	protected final Map<DNFPredicate, GenericQuerySpecification<RawPatternMatcher>> predicates;

	public ViatraQueryableModelStore(ModelStore store, Set<RelationView<?>> relationViews,
									 Set<DNFPredicate> predicates) {
		this.store = store;
		validateViews(store.getDataRepresentations(), relationViews);
		this.relationViews = Collections.unmodifiableSet(relationViews);
		validatePredicates(relationViews, predicates);
		this.predicates = initPredicates(predicates);
	}

	public ViatraQueryableModelStore(Set<DataRepresentation<?, ?>> dataRepresentations,
									 Set<RelationView<?>> relationViews, Set<DNFPredicate> predicates) {
		this(new ModelStoreImpl(dataRepresentations), relationViews, predicates);
	}

	private void validateViews(Set<DataRepresentation<?, ?>> dataRepresentations, Set<RelationView<?>> relationViews) {
		for (RelationView<?> relationView : relationViews) {
			if (!dataRepresentations.contains(relationView.getRepresentation())) {
				throw new IllegalArgumentException(
						DataRepresentation.class.getSimpleName() + " " + relationView.getStringID() + " added to "
								+ QueryableModelStore.class.getSimpleName() + " without a referred representation.");
			}
		}
	}

	private void validatePredicates(Set<RelationView<?>> relationViews, Set<DNFPredicate> predicates) {
		for (DNFPredicate dnfPredicate : predicates) {
			for (DNFAnd clause : dnfPredicate.getClauses()) {
				for (DNFAtom atom : clause.getConstraints()) {
					if (atom instanceof RelationAtom relationAtom) {
						validateRelationAtom(relationViews, dnfPredicate, relationAtom);
					} else if (atom instanceof PredicateAtom predicateAtom) {
						validatePredicateAtom(predicates, dnfPredicate, predicateAtom);
					}
				}
			}
		}
	}

	private void validateRelationAtom(Set<RelationView<?>> relationViews, DNFPredicate dnfPredicate,
									  RelationAtom relationAtom) {
		if (!relationViews.contains(relationAtom.view())) {
			throw new IllegalArgumentException(DNFPredicate.class.getSimpleName() + " "
					+ dnfPredicate.getUniqueName() + " contains reference to a view of "
					+ relationAtom.view().getRepresentation().getName()
					+ " that is not in the model.");
		}
	}

	private void validatePredicateAtom(Set<DNFPredicate> predicates, DNFPredicate dnfPredicate,
									   PredicateAtom predicateAtom) {
		if (!predicates.contains(predicateAtom.getReferred())) {
			throw new IllegalArgumentException(
					DNFPredicate.class.getSimpleName() + " " + dnfPredicate.getUniqueName()
							+ " contains reference to a predicate "
							+ predicateAtom.getReferred().getName()
							+ "that is not in the model.");
		}
	}

	private Map<DNFPredicate, GenericQuerySpecification<RawPatternMatcher>> initPredicates(Set<DNFPredicate> predicates) {
		Map<DNFPredicate, GenericQuerySpecification<RawPatternMatcher>> result = new HashMap<>();
		Map<DNFPredicate, SimplePQuery> dnf2PQueryMap = new HashMap<>();
		for (DNFPredicate dnfPredicate : predicates) {
			GenericQuerySpecification<RawPatternMatcher> query =
					DNF2PQuery.translate(dnfPredicate, dnf2PQueryMap).build();
			result.put(dnfPredicate, query);
		}

		return result;
	}

	@Override
	public Set<DataRepresentation<?, ?>> getDataRepresentations() {
		return store.getDataRepresentations();
	}

	@Override
	public Set<RelationView<?>> getViews() {
		return this.relationViews;
	}

	@Override
	public Set<DNFPredicate> getPredicates() {
		return predicates.keySet();
	}

	@Override
	public QueryableModel createModel() {
		return new ViatraQueryableModel(this, this.store.createModel(), predicates);
	}

	@Override
	public QueryableModel createModel(long state) {
		return new ViatraQueryableModel(this, this.store.createModel(state), predicates);
	}

	@Override
	public synchronized Set<Long> getStates() {
		return this.store.getStates();
	}

	@Override
	public synchronized ModelDiffCursor getDiffCursor(long from, long to) {
		return this.store.getDiffCursor(from, to);
	}
}
