package tools.refinery.language.mapping;

import java.util.HashMap;
import java.util.Map;

import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.PredicateDefinition;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;
import tools.refinery.store.query.building.DNFNode;
import tools.refinery.store.query.building.DNFPredicate;
import tools.refinery.store.query.building.Variable;

public class Mappings {
	private Map<PredicateDefinition, DNFPredicate> predicateMap;
	private Map<tools.refinery.language.model.problem.Variable, Variable> variableMap;
	private Map<Node, Integer> nodeMap;
	private Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> relationMap;

	public Mappings(Map<PredicateDefinition, DNFPredicate> predicateMap,
			Map<tools.refinery.language.model.problem.Variable, Variable> variableMap, Map<Node, Integer> nodeMap,
			Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> relationMap) {
		this.predicateMap = predicateMap;
		this.variableMap = variableMap;
		this.nodeMap = nodeMap;
		this.relationMap = relationMap;
	}

	public Map<tools.refinery.language.model.problem.Variable, Variable> getVariableMap() {
		return variableMap;
	}

	public Map<Node, Integer> getNodeMap() {
		return nodeMap;
	}

	public Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> getRelationMap() {
		return relationMap;
	}

	public Map<PredicateDefinition, DNFPredicate> getPredicateMap() {
		return predicateMap;
	}

}
