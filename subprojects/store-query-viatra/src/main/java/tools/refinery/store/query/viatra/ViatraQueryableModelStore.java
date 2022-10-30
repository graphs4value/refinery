package tools.refinery.store.query.viatra;

import org.eclipse.viatra.query.runtime.api.GenericQuerySpecification;
import tools.refinery.store.model.ModelDiffCursor;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreImpl;
import tools.refinery.store.model.RelationLike;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.query.*;
import tools.refinery.store.query.atom.*;
import tools.refinery.store.query.viatra.internal.RawPatternMatcher;
import tools.refinery.store.query.viatra.internal.ViatraQueryableModel;
import tools.refinery.store.query.viatra.internal.pquery.DNF2PQuery;
import tools.refinery.store.query.view.RelationView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ViatraQueryableModelStore implements QueryableModelStore {
	protected final ModelStore store;

	protected final Set<RelationView<?>> relationViews;

	protected final Map<DNF, GenericQuerySpecification<RawPatternMatcher>> predicates;

	public ViatraQueryableModelStore(ModelStore store, Set<RelationView<?>> relationViews,
									 Set<DNF> predicates) {
		this.store = store;
		validateViews(store.getDataRepresentations(), relationViews);
		this.relationViews = Collections.unmodifiableSet(relationViews);
		validatePredicates(relationViews, predicates);
		this.predicates = initPredicates(predicates);
	}

	public ViatraQueryableModelStore(Set<DataRepresentation<?, ?>> dataRepresentations,
									 Set<RelationView<?>> relationViews, Set<DNF> predicates) {
		this(new ModelStoreImpl(dataRepresentations), relationViews, predicates);
	}

	private void validateViews(Set<DataRepresentation<?, ?>> dataRepresentations, Set<RelationView<?>> relationViews) {
		for (RelationView<?> relationView : relationViews) {
			if (!dataRepresentations.contains(relationView.getRepresentation())) {
				throw new IllegalArgumentException("%s %s added to %s without a referred representation.".formatted(
						DataRepresentation.class.getSimpleName(), relationView.getName(),
						QueryableModelStore.class.getSimpleName()));
			}
		}
	}

	private void validatePredicates(Set<RelationView<?>> relationViews, Set<DNF> predicates) {
		for (DNF dnfPredicate : predicates) {
			for (DNFAnd clause : dnfPredicate.getClauses()) {
				for (DNFAtom atom : clause.constraints()) {
					if (atom instanceof RelationViewAtom relationViewAtom) {
						validateRelationAtom(relationViews, dnfPredicate, relationViewAtom);
					} else if (atom instanceof CallAtom<?> queryCallAtom) {
						validatePredicateAtom(predicates, dnfPredicate, queryCallAtom);
					} else if (atom instanceof CountNotEqualsAtom<?> countNotEqualsAtom) {
						validateCountNotEqualsAtom(predicates, dnfPredicate, countNotEqualsAtom);
					} else if (!(atom instanceof EquivalenceAtom || atom instanceof ConstantAtom)) {
						throw new IllegalArgumentException("Unknown constraint: " + atom.toString());
					}
				}
			}
		}
	}

	private void validateRelationAtom(Set<RelationView<?>> relationViews, DNF dnfPredicate,
									  RelationViewAtom relationViewAtom) {
		if (!relationViews.contains(relationViewAtom.getTarget())) {
			throw new IllegalArgumentException(
					"%s %s contains reference to a view %s that is not in the model.".formatted(
							DNF.class.getSimpleName(), dnfPredicate.getUniqueName(),
							relationViewAtom.getTarget().getName()));
		}
	}

	private void validatePredicateReference(Set<DNF> predicates, DNF dnfPredicate, RelationLike target) {
		if (!(target instanceof DNF dnfTarget) || !predicates.contains(dnfTarget)) {
			throw new IllegalArgumentException(
					"%s %s contains reference to a predicate %s that is not in the model.".formatted(
							DNF.class.getSimpleName(), dnfPredicate.getUniqueName(), target.getName()));
		}
	}

	private void validatePredicateAtom(Set<DNF> predicates, DNF dnfPredicate, CallAtom<?> queryCallAtom) {
		validatePredicateReference(predicates, dnfPredicate, queryCallAtom.getTarget());
	}

	private void validateCountNotEqualsAtom(Set<DNF> predicates, DNF dnfPredicate,
											CountNotEqualsAtom<?> countNotEqualsAtom) {
		validatePredicateReference(predicates, dnfPredicate, countNotEqualsAtom.mayTarget());
		validatePredicateReference(predicates, dnfPredicate, countNotEqualsAtom.mustTarget());
	}

	private Map<DNF, GenericQuerySpecification<RawPatternMatcher>> initPredicates(Set<DNF> predicates) {
		Map<DNF, GenericQuerySpecification<RawPatternMatcher>> result = new HashMap<>();
		var dnf2PQuery = new DNF2PQuery();
		for (DNF dnfPredicate : predicates) {
			GenericQuerySpecification<RawPatternMatcher> query = dnf2PQuery.translate(dnfPredicate).build();
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
	public Set<DNF> getPredicates() {
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
