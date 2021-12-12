package tools.refinery.language.mapping;

import java.util.Map;

import tools.refinery.language.model.problem.Node;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;

public class PartialModelMapperDTO {
	private Model model;
	private Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> relationMap;
	private Map<Node, Integer> nodeMap;
	private Map<Node, Integer> enumNodeMap;
	private Map<Node, Integer> uniqueNodeMap;
	private Map<Node, Integer> newNodeMap;
	
	public PartialModelMapperDTO(Model model,
			Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> relationMap,
			Map<Node, Integer> nodeMap,
			Map<Node, Integer> enumNodeMap,
			Map<Node, Integer> uniqueNodeMap,
			Map<Node, Integer> newNodeMap) {
		this.model = model;
		this.relationMap = relationMap;
		this.nodeMap = nodeMap;
		this.enumNodeMap = enumNodeMap;
		this.uniqueNodeMap = uniqueNodeMap;
		this.newNodeMap = newNodeMap;
	}
	
	public Model getModel() { 
		return this.model; 
	}
	public Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> getRelationMap(){
		return this.relationMap;
	}
	public Map<Node, Integer> getNodeMap() {
		return this.nodeMap;
	}
	public Map<Node, Integer> getEnumNodeMap() {
		return this.enumNodeMap;
	}
	public Map<Node, Integer> getUniqueNodeMap() {
		return this.uniqueNodeMap;
	}
	public Map<Node, Integer> getNewNodeMap() {
		return this.newNodeMap;
	}

	public void setModel(Model model) {
		this.model = model;
	}
}
