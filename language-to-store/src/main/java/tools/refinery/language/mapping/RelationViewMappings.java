package tools.refinery.language.mapping;

import java.util.Map;
import java.util.Set;

import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;
import tools.refinery.store.query.building.DNFPredicate;
import tools.refinery.store.query.view.RelationView;

public class RelationViewMappings {
	Set<DNFPredicate> directPredicates;
	Map<RelationView<TruthValue>, DNFPredicate> helperDirectPredicates;
	Map<Relation<TruthValue>, RelationView<TruthValue>> mayViewMap;
	Map<Relation<TruthValue>, RelationView<TruthValue>> mustViewMap;
	Set<Relation<TruthValue>> relations;

	public RelationViewMappings(Set<DNFPredicate> directPredicates,
			Map<RelationView<TruthValue>, DNFPredicate> helperDirectPredicates,
			Map<Relation<TruthValue>, RelationView<TruthValue>> mayViewMap,
			Map<Relation<TruthValue>, RelationView<TruthValue>> mustViewMap, Set<Relation<TruthValue>> relations) {
		super();
		this.directPredicates = directPredicates;
		this.helperDirectPredicates = helperDirectPredicates;
		this.mayViewMap = mayViewMap;
		this.mustViewMap = mustViewMap;
		this.relations = relations;
	}

	public Set<DNFPredicate> getDirectPredicates() {
		return directPredicates;
	}
	
	public Map<RelationView<TruthValue>, DNFPredicate> getHelperDirectPredicates() {
		return helperDirectPredicates;
	}

	public Map<Relation<TruthValue>, RelationView<TruthValue>> getMayViewMap() {
		return mayViewMap;
	}

	public Map<Relation<TruthValue>, RelationView<TruthValue>> getMustViewMap() {
		return mustViewMap;
	}

	public Set<Relation<TruthValue>> getRelations() {
		return relations;
	}
}
