package tools.refinery.language.mapping;

import java.util.HashMap;
import java.util.Map;

import tools.refinery.language.model.problem.Node;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;
import tools.refinery.store.query.building.Variable;

public class Mappings {
	private Map<tools.refinery.language.model.problem.Variable, Variable> variableMap;
	private Map<Node, Integer> nodeMap;
	private Map<Node, Variable> nodeVariableMap;
	private Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> relationMap;

	public Mappings(Map<tools.refinery.language.model.problem.Variable, Variable> variableMap,
			Map<Node, Integer> nodeMap, Map<Node, Variable> nodeVariableMap,
			Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> relationMap) {
		this.variableMap = variableMap;
		this.nodeMap = nodeMap;
		this.nodeVariableMap = nodeVariableMap;
		this.relationMap = relationMap;
	}

	public Map<tools.refinery.language.model.problem.Variable, Variable> getVariableMap() {
		return variableMap;
	}

	public Map<Node, Integer> getNodeMap() {
		return nodeMap;
	}

	public Map<Node, Variable> getNodeVariableMap() {
		return nodeVariableMap;
	}

	public Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> getRelationMap() {
		return relationMap;
	}
	
	
}
