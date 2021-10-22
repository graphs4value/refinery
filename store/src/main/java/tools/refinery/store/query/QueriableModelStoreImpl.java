package tools.refinery.store.query;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;

import tools.refinery.store.model.ModelDiffCursor;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreImpl;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.query.building.DNFAnd;
import tools.refinery.store.query.building.DNFAtom;
import tools.refinery.store.query.building.DNFPredicate;
import tools.refinery.store.query.building.PredicateAtom;
import tools.refinery.store.query.building.RelationAtom;
import tools.refinery.store.query.internal.DNF2PQuery;
import tools.refinery.store.query.internal.RawPatternMatcher;
import tools.refinery.store.query.view.RelationView;

public class QueriableModelStoreImpl implements QueriableModelStore {
	protected final ModelStore store;
	protected final Set<RelationView<?>> relationViews;
	protected final Map<DNFPredicate, GenericQuerySpecification<RawPatternMatcher>> predicates;

	public QueriableModelStoreImpl(Set<DataRepresentation<?, ?>> dataRepresentations,
			Set<RelationView<?>> relationViews, Set<DNFPredicate> predicates) {
		this.store = new ModelStoreImpl(dataRepresentations);
		validateViews(dataRepresentations, relationViews);
		this.relationViews = Collections.unmodifiableSet(relationViews);
		validatePredicates(relationViews, predicates);
		this.predicates = initPredicates(predicates);
	}

	private void validateViews(Set<DataRepresentation<?, ?>> dataRepresentations, Set<RelationView<?>> relationViews) {
		for (RelationView<?> relationView : relationViews) {
			// TODO: make it work for non-relation representation?
			if (!dataRepresentations.contains(relationView.getRepresentation())) {
				throw new IllegalArgumentException(
						DataRepresentation.class.getSimpleName() + " " + relationView.getStringID() + " added to "
								+ QueriableModelStore.class.getSimpleName() + " without a referred representation.");
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
		if (!relationViews.contains(relationAtom.getView())) {
			throw new IllegalArgumentException(DNFPredicate.class.getSimpleName() + " "
					+ dnfPredicate.getUniqueName() + " contains reference to a view of "
					+ relationAtom.getView().getRepresentation().getName()
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

		for (DNFPredicate dnfPredicate : predicates) {
			GenericQuerySpecification<RawPatternMatcher> query = DNF2PQuery.translate(dnfPredicate).build();
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
	public QueriableModel createModel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public QueriableModel createModel(long state) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Long> getStates() {
		return this.store.getStates();
	}

	@Override
	public ModelDiffCursor getDiffCursor(long from, long to) {
		return this.store.getDiffCursor(from, to);
	}
}
