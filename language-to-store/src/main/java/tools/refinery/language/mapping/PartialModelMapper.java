package tools.refinery.language.mapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.common.util.EList;

import tools.refinery.language.ProblemUtil;
import tools.refinery.language.model.problem.Assertion;
import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.language.model.problem.ClassDeclaration;
import tools.refinery.language.model.problem.EnumDeclaration;
import tools.refinery.language.model.problem.LogicValue;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.NodeAssertionArgument;
import tools.refinery.language.model.problem.PredicateDefinition;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ReferenceDeclaration;
import tools.refinery.language.model.problem.Statement;
import tools.refinery.language.model.problem.UniqueDeclaration;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreImpl;
import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;


public class PartialModelMapper {
	
	private int nodeIter;
	public PartialModelMapper() {
		this.nodeIter = 0;
	}
	
	public PartialModelMapperDTO transformProblem(Problem problem) throws Exception {
		PartialModelMapperDTO pmmDTO = initTransform(problem);
		
		Optional<Problem> builtinProblem = ProblemUtil.getBuiltInLibrary(problem);
		if (builtinProblem.isEmpty()) throw new Exception("builtin.problem not found");
		PartialModelMapperDTO builtinProblemDTO = initTransform(builtinProblem.get());
		pmmDTO.getRelationMap().putAll(builtinProblemDTO.getRelationMap());
		pmmDTO.getNodeMap().putAll(builtinProblemDTO.getNodeMap());
		pmmDTO.getEnumNodeMap().putAll(builtinProblemDTO.getEnumNodeMap()); //így most valami nem stimmel
		pmmDTO.getNewNodeMap().putAll(builtinProblemDTO.getNewNodeMap());
		pmmDTO.getUniqueNodeMap().putAll(builtinProblemDTO.getUniqueNodeMap());
		
		//Definition of store and model
		ModelStore store = new ModelStoreImpl(new HashSet<>(pmmDTO.getRelationMap().values()));
		Model model = store.createModel();
		pmmDTO.setModel(model);
		
		Map<Node,Integer> allNodesMap = mergeNodeMaps(pmmDTO.getEnumNodeMap(),
													  pmmDTO.getUniqueNodeMap(),
													  pmmDTO.getNewNodeMap(),
													  pmmDTO.getNodeMap());
		
		//Filling up the relations with unknown truth values 
		for (tools.refinery.language.model.problem.Relation relation : pmmDTO.getRelationMap().keySet()) {
			if(!(relation instanceof PredicateDefinition pd && pd.isError())) {
				Relation<TruthValue> r = pmmDTO.getRelationMap().get(relation);
				if(r.getArity() == 1)
				for(Integer i : allNodesMap.values()) {
					pmmDTO.getModel().put(r, Tuple.of(i), TruthValue.UNKNOWN);
				}
				else if(r.getArity() == 2) 
				for(Integer i : allNodesMap.values()) {
					for (Integer j : allNodesMap.values()) {
						pmmDTO.getModel().put(r, Tuple.of(i,j), TruthValue.UNKNOWN);
					}
				}
				else throw new Exception("Relation with arity above 2 is not supported");
			}
		}
		
		//Filling up the exists
		tools.refinery.language.model.problem.Relation existsRelation = null;
		for (tools.refinery.language.model.problem.Relation r : builtinProblemDTO.getRelationMap().keySet()) {
			if (r.getName().equals("exists")) existsRelation = r;
		}
		if(existsRelation.equals(null)) throw new Exception("exists not found");
		for (Node n : allNodesMap.keySet()) {
			if(pmmDTO.getNewNodeMap().containsKey(n)) {
				pmmDTO.getModel().put(builtinProblemDTO.getRelationMap().get(existsRelation),
									  Tuple.of(allNodesMap.get(n)),
									  TruthValue.UNKNOWN);
			}
			else {
				pmmDTO.getModel().put(builtinProblemDTO.getRelationMap().get(existsRelation),
						  Tuple.of(allNodesMap.get(n)),
						  TruthValue.TRUE);
			}
		}
		
		//Filling up the equals
		tools.refinery.language.model.problem.Relation equalsRelation = null;
		for (tools.refinery.language.model.problem.Relation r : builtinProblemDTO.getRelationMap().keySet()) {
			if (r.getName().equals("equals")) equalsRelation = r;
		}
		if(equalsRelation.equals(null)) throw new Exception("equals not found");
		for (Node n1 : allNodesMap.keySet()) {
			for(Node n2 : allNodesMap.keySet()) {
				if(n1.equals(n2)) {
					if(pmmDTO.getNewNodeMap().containsKey(n1)) {
						pmmDTO.getModel().put(builtinProblemDTO.getRelationMap().get(equalsRelation),
											  Tuple.of(allNodesMap.get(n1),allNodesMap.get(n2)),
											  TruthValue.UNKNOWN);
					}
					else {
						pmmDTO.getModel().put(builtinProblemDTO.getRelationMap().get(equalsRelation),
								  Tuple.of(allNodesMap.get(n1),allNodesMap.get(n2)),
								  TruthValue.TRUE);
					}
				}
				else {
					pmmDTO.getModel().put(builtinProblemDTO.getRelationMap().get(equalsRelation),
							  Tuple.of(allNodesMap.get(n1),allNodesMap.get(n2)),
							  TruthValue.FALSE);
				}
			}
		}
		
		//Transforming the assertions
		processAssertions(problem, pmmDTO, allNodesMap);
		processAssertions(builtinProblem.get(), pmmDTO, allNodesMap);
		
		//throw new UnsupportedOperationException();
		return pmmDTO;
	}

	private void processAssertions(Problem problem, PartialModelMapperDTO pmmDTO, Map<Node, Integer> allNodesMap) {
		for(Statement s : problem.getStatements()) {
			if(s instanceof Assertion assertion) {
				Relation<TruthValue> r1 = pmmDTO.getRelationMap().get(assertion.getRelation());
				int i = 0;
				int[] integers = new int[assertion.getArguments().size()];
				for (AssertionArgument aa : assertion.getArguments()) {
					if (aa instanceof NodeAssertionArgument nas) {
						integers[i] = allNodesMap.get(nas.getNode());
						i++;
					}
				}
				pmmDTO.getModel().put(r1, Tuple.of(integers), logicValueToTruthValue(assertion.getValue()));
			}
			else if (s instanceof ClassDeclaration cd) {
				if(!cd.isAbstract())
				pmmDTO.getModel().put(pmmDTO.getRelationMap().get(cd),
									  Tuple.of(pmmDTO.getNewNodeMap().get(cd.getNewNode())),
									  TruthValue.TRUE);
			}
			else if (s instanceof EnumDeclaration ed) {
				for (Node n : ed.getLiterals()) {
					pmmDTO.getModel().put(pmmDTO.getRelationMap().get(ed),
							  Tuple.of(pmmDTO.getEnumNodeMap().get(n)),
							  TruthValue.TRUE);
				}
			}
		}
	}
	
	public PartialModelMapperDTO initTransform(Problem problem) {
		//Defining needed Maps
		Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> relationMap = new HashMap<>();
		Map<Node, Integer> enumNodeMap = new HashMap<>();
		Map<Node, Integer> uniqueNodeMap = new HashMap<>();
		Map<Node, Integer> newNodeMap = new HashMap<>();
		
		//Definition of Relations, filling up the enumNodeMap, uniqueNodeMap, newNodeMap
		EList<Statement> statements = problem.getStatements();
		for (Statement s : statements) {
			if (s instanceof ClassDeclaration cd) {
				Relation<TruthValue> r1 = new Relation<>(cd.getName(), 1, TruthValue.FALSE);
				relationMap.put(cd, r1);
				if(!cd.isAbstract()) newNodeMap.put(cd.getNewNode(), this.nodeIter++);
				EList<ReferenceDeclaration> refDeclList = cd.getReferenceDeclarations();
				for (ReferenceDeclaration refDec : refDeclList) {
					Relation<TruthValue> r2 = new Relation<>(refDec.getName(), 2, TruthValue.FALSE);
					relationMap.put(refDec, r2);
				}
			}
			else if (s instanceof EnumDeclaration ed) {
				Relation<TruthValue> r = new Relation<>(ed.getName(), 1, TruthValue.FALSE);
				relationMap.put(ed, r);
				EList<Node> nodeList = ed.getLiterals();
				for (Node n : ed.getLiterals()) {
					enumNodeMap.put(n, nodeIter++);
				}
			}
			else if (s instanceof UniqueDeclaration ud) {
				for (Node n : ud.getNodes()) {
					uniqueNodeMap.put(n, this.nodeIter++);
				}
			}
			else if (s instanceof PredicateDefinition pd) {
				Relation<TruthValue> r = new Relation<>(pd.getName(), 1, TruthValue.FALSE);
				relationMap.put(pd, r);
			}
		}
		
		
		
		
		//Filling the nodeMap up
		Map<Node, Integer> nodeMap = new HashMap<>();
		for(Node n : problem.getNodes()) {
			nodeMap.put(n, this.nodeIter++);
		}
		
		return new PartialModelMapperDTO(null,relationMap,nodeMap,enumNodeMap,uniqueNodeMap,newNodeMap);
	}

	private Map<Node, Integer> mergeNodeMaps(Map<Node, Integer> enumNodeMap,
											 Map<Node, Integer> uniqueNodeMap,
											 Map<Node, Integer> newNodeMap,
											 Map<Node, Integer> nodeMap) {
		Map<Node, Integer> out = new HashMap<>(enumNodeMap);
		for (Node n : uniqueNodeMap.keySet()) out.put(n, uniqueNodeMap.get(n));
		for (Node n : newNodeMap.keySet()) out.put(n, newNodeMap.get(n));
		for (Node n : nodeMap.keySet()) out.put(n, nodeMap.get(n));
		return out;
	}

	private TruthValue logicValueToTruthValue(LogicValue value) {
		if(value.equals(LogicValue.TRUE)) return TruthValue.TRUE;
		else if(value.equals(LogicValue.FALSE)) return TruthValue.FALSE;
		else if(value.equals(LogicValue.UNKNOWN)) return TruthValue.UNKNOWN;
		else return TruthValue.ERROR;
	}
}
