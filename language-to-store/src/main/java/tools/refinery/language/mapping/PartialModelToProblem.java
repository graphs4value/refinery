package tools.refinery.language.mapping;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import com.google.inject.Inject;
import com.google.inject.Provider;

import tools.refinery.language.model.ProblemUtil;
import tools.refinery.language.model.problem.Assertion;
import tools.refinery.language.model.problem.ClassDeclaration;
import tools.refinery.language.model.problem.EnumDeclaration;
import tools.refinery.language.model.problem.LogicValue;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.NodeAssertionArgument;
import tools.refinery.language.model.problem.Parameter;
import tools.refinery.language.model.problem.PredicateDefinition;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.ProblemFactory;
import tools.refinery.language.model.problem.Statement;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;

public class PartialModelToProblem {
	
	@Inject
	private Provider<ResourceSet> resourceSetProvider;
	
	public Problem transformModelToProblem(Model model) throws ModelToStoreException{
		Problem problem = ProblemFactory.eINSTANCE.createProblem();
		ResourceSet resourceSet = resourceSetProvider.get();
		Resource resource = resourceSet.createResource(URI.createFileURI("transform.problem"));
		resource.getContents().add(problem);
		
		Optional<Problem> builtin = ProblemUtil.getBuiltInLibrary(problem);
		if (builtin.isEmpty()) throw new ModelToStoreException("builtin problem not found");
		Optional<ClassDeclaration> nodeType = ProblemUtil.getNodeClassDeclaration(problem);
		if (nodeType.isEmpty()) throw new ModelToStoreException("node class declaration not found in builtin problem");
		
		ArrayList<Integer> nodeList = new ArrayList<>();
		Set<DataRepresentation<?,?>> drSet = model.getDataRepresentations();
		for(DataRepresentation<?,?> dr : drSet ) {
			if(dr instanceof Relation<?>) {
				@SuppressWarnings("unchecked")
				Relation<TruthValue> relation = (Relation<TruthValue>) dr;
				
				Optional<Statement> statement = problemContainsRelation(builtin.get(), relation);
				if(statement.isPresent()) {
					problem.getStatements().add(statement.get());
				}
				else {
					PredicateDefinition pred = ProblemFactory.eINSTANCE.createPredicateDefinition();
					pred.setName(relation.getName());
					createParametersForPredicate(relation, pred, nodeType.get());
					problem.getStatements().add(pred);
				}
				fillNodesFromRelation(model, problem, nodeList, relation);
			}
		}
		
		for(DataRepresentation<?,?> dr : drSet ) {
			if(dr instanceof Relation<?>) {
				@SuppressWarnings("unchecked")
				Relation<TruthValue> relation = (Relation<TruthValue>) dr;
				
				createAssertionsForRelation(model, problem, relation);
				fillRelationWithFalseAssertions(problem, relation);
			}
		}
		
		return problem;
	}
	
	private void fillRelationWithFalseAssertions(Problem problem, Relation<TruthValue> relation) throws ModelToStoreException {
		if(relation.getArity()==1) {
			for(Node n : problem.getNodes()) {
				if(!existsAssertionInProblem(problem, relation,
						Tuple.of(Integer.parseInt(n.getName())))) {
					createAssertion(problem, relation,
									Tuple.of(Integer.parseInt(n.getName())),
									LogicValue.FALSE);
				}
			}
		}
		else if(relation.getArity()==2) {
			for(Node n1 : problem.getNodes()) {
				for(Node n2 : problem.getNodes()) {
					if(!existsAssertionInProblem(problem, relation,
							Tuple.of(Integer.parseInt(n1.getName()), Integer.parseInt(n2.getName())))) {
						createAssertion(problem, relation,
										Tuple.of(Integer.parseInt(n1.getName()), Integer.parseInt(n2.getName())),
										LogicValue.FALSE);
					}
				}
			}
		}
		else throw new ModelToStoreException("Unsupported number of arguments in relation.");
	}

	private boolean existsAssertionInProblem(Problem problem, Relation<TruthValue> relation, Tuple tuple) {
		for(Statement s : problem.getStatements()) {
			if(s instanceof Assertion a) {
				if(a.getRelation().getName().equals(relation.getName()) &&
						tuple.getSize() == a.getArguments().size()) {
					for(int i = 0; i < tuple.getSize(); i++) {
						if(a.getArguments().get(i) instanceof NodeAssertionArgument naa) {
							if(Integer.parseInt(naa.getNode().getName()) != tuple.get(i)) return false;
						}
					}
					return true;
				}
			}
		}
		
		
		return false;
	}

	private void createParametersForPredicate(Relation<TruthValue> relation,
											  PredicateDefinition pred,
											  ClassDeclaration nodeType) {
		for (int i = 0; i < relation.getArity(); i++) {
			Parameter parameter = ProblemFactory.eINSTANCE.createParameter();
			parameter.setParameterType(nodeType);
			parameter.setName("p" + Integer.toString(i+1));
			pred.getParameters().add(parameter);
		}
	}
	
	private void fillNodesFromRelation(Model model,
						   Problem problem,
						   ArrayList<Integer> nodeList,
						   Relation<TruthValue> storeRelation) {
		var cursor = model.getAll(storeRelation);
		while(cursor.move()) {
			Tuple tuple = cursor.getKey();
			for(int i = 0; i < tuple.getSize(); i++) {
				Node node = ProblemFactory.eINSTANCE.createNode();
				node.setName(Integer.toString(tuple.get(i)));
				if(!nodeList.contains(tuple.get(i))) {
					nodeList.add(tuple.get(i));
					problem.getNodes().add(node);
				}
			}
		}
	}

	private void createAssertion(Problem problem, Relation<TruthValue> relation, Tuple tuple, LogicValue value) throws ModelToStoreException {
		Assertion assertion = ProblemFactory.eINSTANCE.createAssertion();
		
		Optional<Statement> statement = problemContainsRelation(problem, relation);
		if(statement.isEmpty()) throw new ModelToStoreException("Relation not found in problem.");
		if(statement.get() instanceof ClassDeclaration cd) {
			assertion.setRelation(cd);
		}
		else if(statement.get() instanceof EnumDeclaration ed) {
			assertion.setRelation(ed);
		}
		else if(statement.get() instanceof PredicateDefinition pd) {
			assertion.setRelation(pd);
		}
		else throw new ModelToStoreException("Not supported relation type.");
	
		for (int i = 0; i < tuple.getSize(); i++) {
			Optional<Node> node = findNodeInProblem(problem, Integer.toString(tuple.get(i)));
			if(node.isEmpty()) throw new ModelToStoreException("Node not found when trying to create assertion.");
		
			NodeAssertionArgument argument = ProblemFactory.eINSTANCE.createNodeAssertionArgument();
			argument.setNode(node.get());
		
			assertion.getArguments().add(argument);
		}
		
		assertion.setValue(value);
		problem.getStatements().add(assertion);
	}
	
	private void createAssertionsForRelation(Model model,
								  Problem problem,
								  Relation<TruthValue> storeRelation) throws ModelToStoreException {
		var cursor = model.getAll(storeRelation);
		while(cursor.move()) {
			createAssertion(problem, storeRelation,
							cursor.getKey(),
							truthValuetoLogicValue(cursor.getValue()));
		}
	}

	private Optional<Node> findNodeInProblem(Problem problem, String nodeName) {
		for (Node n : problem.getNodes()) {
			if (n.getName().equals(nodeName)) return Optional.of(n);
		}
		return Optional.empty();
	}

	private Optional<Statement> problemContainsRelation(Problem problem, Relation<TruthValue> relation) {
		for(Statement s : problem.getStatements()) {
			if(s instanceof ClassDeclaration cd &&
					cd.getName().equals(relation.getName()))
						return Optional.of(cd);
			else if(s instanceof EnumDeclaration ed &&
						ed.getName().equals(relation.getName()))
							return Optional.of(ed);
			else if(s instanceof PredicateDefinition pd &&
						pd.getName().equals(relation.getName()))
							return Optional.of(pd);
		}
		return Optional.empty();
	}

	// Exchange method from TruthValue to LogicValue
	private LogicValue truthValuetoLogicValue(TruthValue value) {
		if(value.equals(TruthValue.TRUE))
			return LogicValue.TRUE;
		else if(value.equals(TruthValue.FALSE))
			return LogicValue.FALSE;
		else if(value.equals(TruthValue.UNKNOWN))
			return LogicValue.UNKNOWN;
		else return LogicValue.ERROR;
	}
}
