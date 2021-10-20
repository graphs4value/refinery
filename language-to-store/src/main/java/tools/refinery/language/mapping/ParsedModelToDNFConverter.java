package tools.refinery.language.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.refinery.language.model.problem.Argument;
import tools.refinery.language.model.problem.Atom;
import tools.refinery.language.model.problem.CompoundLiteral;
import tools.refinery.language.model.problem.Conjunction;
import tools.refinery.language.model.problem.ImplicitVariable;
import tools.refinery.language.model.problem.Literal;
import tools.refinery.language.model.problem.LogicConstant;
import tools.refinery.language.model.problem.NegativeLiteral;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.Parameter;
import tools.refinery.language.model.problem.PredicateDefinition;
import tools.refinery.language.model.problem.ValueLiteral;
import tools.refinery.language.model.problem.VariableOrNode;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;
import tools.refinery.store.query.building.DNFAnd;
import tools.refinery.store.query.building.DNFAtom;
import tools.refinery.store.query.building.DNFPredicate;
import tools.refinery.store.query.building.DNFPredicateCallAtom;
import tools.refinery.store.query.building.DirectRelationAtom;
import tools.refinery.store.query.building.Variable;

public class ParsedModelToDNFConverter {
	public Map<PredicateDefinition, DNFPredicate> transformPred(Set<PredicateDefinition> predicates,
			Map<Node, Integer> nodeMap,
			Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> relationMap) throws Exception {
		Mappings mappings = new Mappings(new HashMap<>(), nodeMap, new HashMap<>(), relationMap);
		Map<PredicateDefinition, DNFPredicate> predicateMapping = new HashMap<>();
		for (PredicateDefinition predicate : predicates) {
			predicateMapping.put(predicate, convertPredicate(predicate, mappings));
		}
		return predicateMapping;
	}

	protected DNFPredicate convertPredicate(PredicateDefinition predicateDefinition, Mappings mappings) throws Exception {
		List<Variable> parameters = new ArrayList<>();
		for (Parameter parameter : predicateDefinition.getParameters()) {
			parameters.add(convertVariable(parameter, mappings));
		}
		List<DNFAnd> conjunctions = new ArrayList<>();
		for (Conjunction conjunction : predicateDefinition.getBodies()) {
			conjunctions.add(convertConjunction(conjunction, mappings));
		}
		return new DNFPredicate(predicateDefinition.getName(), parameters, conjunctions);

	}

	protected Variable convertVariable(tools.refinery.language.model.problem.Variable variable, Mappings mappings) {
		Variable dnfVar = mappings.getVariableMap().get(variable);
		if (dnfVar == null) {
			dnfVar = new Variable(variable.getName());
			mappings.getVariableMap().put(variable, dnfVar);
		}
		return dnfVar;
	}

	protected Variable convertNode(Node node, Mappings mappings) {
		Variable dnfVar = mappings.getNodeVariableMap().get(node);
		if (dnfVar == null) {
			dnfVar = new Variable(node.getName());
			mappings.getNodeVariableMap().put(node, dnfVar);
		}
		return dnfVar;
	}
	
	protected TruthValue convertLogicConstant(LogicConstant logicConstant) {
		return TruthValue.valueOf(logicConstant.getValue().toString());
	}

	protected Variable convertArgument(Argument argument, Mappings mappings) throws Exception {
		if (argument instanceof tools.refinery.language.model.problem.Variable variable) {
			return convertVariable(variable, mappings);
		} else if (argument instanceof Node node) {
			return convertNode(node, mappings);
		}
		throw new Exception("Unknown type in Argument");
	}

	protected DNFAnd convertConjunction(Conjunction conjunction, Mappings mappings) throws Exception {
		Set<Variable> quantifiedVariables = new HashSet<>();
		for (ImplicitVariable variable : conjunction.getImplicitVariables()) {
			quantifiedVariables.add(convertVariable(variable, mappings));
		}
		List<DNFAtom> constraints = new ArrayList<>();
		for (Literal literal : conjunction.getLiterals()) {
			constraints.add(convertLiteral(literal, mappings));
		}
		return new DNFAnd(quantifiedVariables, constraints);
	}

	protected DNFAtom convertLiteral(Literal literal, Mappings mappings) throws Exception {
		if (literal instanceof ValueLiteral valueLiteral) {
			Atom atom = valueLiteral.getAtom();
			List<Variable> substitution = new ArrayList<>();
			for (Argument argument : atom.getArguments()) {
				substitution.add(convertArgument(argument, mappings));
			}
			Relation<TruthValue> relation = mappings.getRelationMap().get(atom.getRelation());
			List<Variable> arguments = new ArrayList<>();
			for (var variable : atom.getArguments()) {
				convertArgument(variable, mappings);
			}
			List<TruthValue> allowedTruthValues = new ArrayList<>();
			for (var truthValue : valueLiteral.getValues()) {
				allowedTruthValues.add(convertLogicConstant(truthValue));
			}
			return new DirectRelationAtom(relation, arguments, allowedTruthValues);
		}
		throw new Exception("Unknown type in Literal");
	}
}
