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
					if(statement.get() instanceof ClassDeclaration cd) 
						createAssertions(model, problem, nodeList, relation, cd);
					else if(statement.get() instanceof EnumDeclaration ed)
						createAssertions(model, problem, nodeList, relation, ed);
					else if(statement.get() instanceof PredicateDefinition pd)
						createAssertions(model, problem, nodeList, relation, pd);
				}
				else {
					PredicateDefinition pred = ProblemFactory.eINSTANCE.createPredicateDefinition();
					pred.setName(relation.getName());
				
					createParametersForPredicate(relation, pred, nodeType.get());
					
					problem.getStatements().add(pred);
					
					createAssertions(model, problem, nodeList, relation, pred);
				}
			}
		}
		
		return problem;
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

	private void createAssertions(Model model,
								  Problem problem,
								  ArrayList<Integer> nodeList,
								  Relation<TruthValue> storeRelation,
								  tools.refinery.language.model.problem.Relation languageRelation) {
		var cursor = model.getAll(storeRelation);
		while(cursor.move()) {
			Tuple tuple = cursor.getKey();
		
			Assertion assertion = ProblemFactory.eINSTANCE.createAssertion();
			assertion.setRelation(languageRelation);
		
			for (int i = 0; i < tuple.getSize(); i++) {
				Node node = ProblemFactory.eINSTANCE.createNode();
				node.setName(Integer.toString(tuple.get(i)));
				if(!nodeList.contains(tuple.get(i))) {
					nodeList.add(tuple.get(i));
					problem.getNodes().add(node);
				}
			
				NodeAssertionArgument argument = ProblemFactory.eINSTANCE.createNodeAssertionArgument();
				argument.setNode(node);
			
				assertion.getArguments().add(argument);
			}
		
			assertion.setValue(truthValuetoLogicValue(cursor.getValue()));
			problem.getStatements().add(assertion);
		}
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
