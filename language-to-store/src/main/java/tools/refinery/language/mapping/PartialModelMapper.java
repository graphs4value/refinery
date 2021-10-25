package tools.refinery.language.mapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.common.util.EList;

import tools.refinery.language.model.ProblemUtil;
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
	public PartialModelMapperDTO transformProblem(Problem problem) throws PartialModelMapperException {
		// Defining an integer in order to assign different values to all the nodes
		int[] nodeIter = new int[] { 0 };

		// Getting the relations and the nodes from the given problem
		PartialModelMapperDTO pmmDTO = initTransform(problem, nodeIter);

		// Getting the relations and the nodes from the built in problem
		Optional<Problem> builtinProblem = ProblemUtil.getBuiltInLibrary(problem);
		if (builtinProblem.isEmpty())
			throw new PartialModelMapperException("builtin.problem not found");
		PartialModelMapperDTO builtinProblemDTO = initTransform(builtinProblem.get(), nodeIter);

		// Merging the relation and the nodes from the given problem and from the built
		// in problem
		pmmDTO.getRelationMap().putAll(builtinProblemDTO.getRelationMap());
		pmmDTO.getNodeMap().putAll(builtinProblemDTO.getNodeMap());
		pmmDTO.getEnumNodeMap().putAll(builtinProblemDTO.getEnumNodeMap());
		pmmDTO.getNewNodeMap().putAll(builtinProblemDTO.getNewNodeMap());
		pmmDTO.getUniqueNodeMap().putAll(builtinProblemDTO.getUniqueNodeMap());

		// Definition of store and model
		ModelStore store = new ModelStoreImpl(new HashSet<>(pmmDTO.getRelationMap().values()));
		Model model = store.createModel();
		pmmDTO.setModel(model);

		// Collecting all the nodes in one map
		Map<Node, Integer> allNodesMap = mergeNodeMaps(pmmDTO.getEnumNodeMap(), pmmDTO.getUniqueNodeMap(),
				pmmDTO.getNewNodeMap(), pmmDTO.getNodeMap());

		// Filling up the relations with unknown truth values
		for (tools.refinery.language.model.problem.Relation relation : pmmDTO.getRelationMap().keySet()) {
			if (!(relation instanceof PredicateDefinition pd && pd.isError())) {
				Relation<TruthValue> r = pmmDTO.getRelationMap().get(relation);
				if (r.getArity() == 1)
					for (Integer i : allNodesMap.values()) {
						pmmDTO.getModel().put(r, Tuple.of(i), TruthValue.UNKNOWN);
					}
				else if (r.getArity() == 2)
					for (Integer i : allNodesMap.values()) {
						for (Integer j : allNodesMap.values()) {
							pmmDTO.getModel().put(r, Tuple.of(i, j), TruthValue.UNKNOWN);
						}
					}
				else
					throw new PartialModelMapperException("Relation with arity above 2 is not supported");
			}
		}

		// Filling up the exists
		tools.refinery.language.model.problem.Relation existsRelation = findingRelationInDTO(builtinProblemDTO,
				"exists", "The exists not found in built in problem");
		for (Node n : allNodesMap.keySet()) {
			if (pmmDTO.getNewNodeMap().containsKey(n)) {
				pmmDTO.getModel().put(builtinProblemDTO.getRelationMap().get(existsRelation),
						Tuple.of(allNodesMap.get(n)), TruthValue.UNKNOWN);
			} else {
				pmmDTO.getModel().put(builtinProblemDTO.getRelationMap().get(existsRelation),
						Tuple.of(allNodesMap.get(n)), TruthValue.TRUE);
			}
		}

		// Filling up the equals
		tools.refinery.language.model.problem.Relation equalsRelation = findingRelationInDTO(builtinProblemDTO,
				"equals", "The equals not found in built in problem");
		for (Node n1 : allNodesMap.keySet()) {
			for (Node n2 : allNodesMap.keySet()) {
				if (n1.equals(n2)) {
					if (pmmDTO.getNewNodeMap().containsKey(n1)) {
						pmmDTO.getModel().put(builtinProblemDTO.getRelationMap().get(equalsRelation),
								Tuple.of(allNodesMap.get(n1), allNodesMap.get(n2)), TruthValue.UNKNOWN);
					} else {
						pmmDTO.getModel().put(builtinProblemDTO.getRelationMap().get(equalsRelation),
								Tuple.of(allNodesMap.get(n1), allNodesMap.get(n2)), TruthValue.TRUE);
					}
				} else {
					pmmDTO.getModel().put(builtinProblemDTO.getRelationMap().get(equalsRelation),
							Tuple.of(allNodesMap.get(n1), allNodesMap.get(n2)), TruthValue.FALSE);
				}
			}
		}

		// Transforming the assertions
		processAssertions(problem, pmmDTO, allNodesMap);
		processAssertions(builtinProblem.get(), pmmDTO, allNodesMap);

		return pmmDTO;
	}

	// Searches for and gives back a relation in a PartialModelMapperDTO
	private tools.refinery.language.model.problem.Relation findingRelationInDTO(
			PartialModelMapperDTO partialModelMapperDTO, String searchedRelation, String errorText)
			throws PartialModelMapperException {
		for (tools.refinery.language.model.problem.Relation r : partialModelMapperDTO.getRelationMap().keySet()) {
			if (searchedRelation.equals(r.getName()))
				return r;
		}
		throw new PartialModelMapperException(errorText);
	}

	// Processing assertions and placing them in the model
	private void processAssertions(Problem problem, PartialModelMapperDTO pmmDTO, Map<Node, Integer> allNodesMap) {
		for (Statement s : problem.getStatements()) {
			if (s instanceof Assertion assertion) {
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
			} else if (s instanceof ClassDeclaration cd) {
				if (!cd.isAbstract())
					pmmDTO.getModel().put(pmmDTO.getRelationMap().get(cd),
							Tuple.of(pmmDTO.getNewNodeMap().get(cd.getNewNode())), TruthValue.TRUE);
			} else if (s instanceof EnumDeclaration ed) {
				for (Node n : ed.getLiterals()) {
					pmmDTO.getModel().put(pmmDTO.getRelationMap().get(ed), Tuple.of(pmmDTO.getEnumNodeMap().get(n)),
							TruthValue.TRUE);
				}
			}
		}
	}

	// Getting the relations and nodes from the problem
	private PartialModelMapperDTO initTransform(Problem problem, int[] nodeIter) {
		// Defining needed Maps
		Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> relationMap = new HashMap<>();
		Map<Node, Integer> enumNodeMap = new HashMap<>();
		Map<Node, Integer> uniqueNodeMap = new HashMap<>();
		Map<Node, Integer> newNodeMap = new HashMap<>();

		// Definition of Relations, filling up the enumNodeMap, uniqueNodeMap,
		// newNodeMap
		EList<Statement> statements = problem.getStatements();
		for (Statement s : statements) {
			if (s instanceof ClassDeclaration cd) {
				Relation<TruthValue> r1 = new Relation<>(cd.getName(), 1, TruthValue.FALSE);
				relationMap.put(cd, r1);
				if (!cd.isAbstract())
					newNodeMap.put(cd.getNewNode(), nodeIter[0]++);
				EList<ReferenceDeclaration> refDeclList = cd.getReferenceDeclarations();
				for (ReferenceDeclaration refDec : refDeclList) {
					Relation<TruthValue> r2 = new Relation<>(refDec.getName(), 2, TruthValue.FALSE);
					relationMap.put(refDec, r2);
				}
			} else if (s instanceof EnumDeclaration ed) {
				Relation<TruthValue> r = new Relation<>(ed.getName(), 1, TruthValue.FALSE);
				relationMap.put(ed, r);
				for (Node n : ed.getLiterals()) {
					enumNodeMap.put(n, nodeIter[0]++);
				}
			} else if (s instanceof UniqueDeclaration ud) {
				for (Node n : ud.getNodes()) {
					uniqueNodeMap.put(n, nodeIter[0]++);
				}
			} else if (s instanceof PredicateDefinition pd) {
				Relation<TruthValue> r = new Relation<>(pd.getName(), 1, TruthValue.FALSE);
				relationMap.put(pd, r);
			}
		}

		// Filling the nodeMap up
		Map<Node, Integer> nodeMap = new HashMap<>();
		for (Node n : problem.getNodes()) {
			nodeMap.put(n, nodeIter[0]++);
		}

		return new PartialModelMapperDTO(null, relationMap, nodeMap, enumNodeMap, uniqueNodeMap, newNodeMap);
	}

	// Merging the maps of nodes into one map
	private Map<Node, Integer> mergeNodeMaps(Map<Node, Integer> enumNodeMap, Map<Node, Integer> uniqueNodeMap,
			Map<Node, Integer> newNodeMap, Map<Node, Integer> nodeMap) {
		Map<Node, Integer> out = new HashMap<>();
		out.putAll(enumNodeMap);
		out.putAll(uniqueNodeMap);
		out.putAll(newNodeMap);
		out.putAll(nodeMap);
		return out;
	}

	// Exchange method from LogicValue to TruthValue
	private TruthValue logicValueToTruthValue(LogicValue value) {
		if (value.equals(LogicValue.TRUE))
			return TruthValue.TRUE;
		else if (value.equals(LogicValue.FALSE))
			return TruthValue.FALSE;
		else if (value.equals(LogicValue.UNKNOWN))
			return TruthValue.UNKNOWN;
		else
			return TruthValue.ERROR;
	}

	public class PartialModelMapperException extends Exception {
		private static final long serialVersionUID = 1L;

		public PartialModelMapperException(String errorText) {
			super(errorText);
		}

		public PartialModelMapperException() {
			super();
		}
	}
}
