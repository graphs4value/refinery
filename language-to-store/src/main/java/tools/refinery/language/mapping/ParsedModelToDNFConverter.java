package tools.refinery.language.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.refinery.language.model.problem.Argument;
import tools.refinery.language.model.problem.Atom;
import tools.refinery.language.model.problem.Conjunction;
import tools.refinery.language.model.problem.ImplicitVariable;
import tools.refinery.language.model.problem.Literal;
import tools.refinery.language.model.problem.LogicConstant;
import tools.refinery.language.model.problem.NegativeLiteral;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.Parameter;
import tools.refinery.language.model.problem.PredicateDefinition;
import tools.refinery.language.model.problem.PredicateKind;
import tools.refinery.language.model.problem.ValueLiteral;
import tools.refinery.language.model.problem.VariableOrNode;
import tools.refinery.language.model.problem.VariableOrNodeArgument;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;
import tools.refinery.store.query.building.DNFAnd;
import tools.refinery.store.query.building.DNFAtom;
import tools.refinery.store.query.building.DNFNode;
import tools.refinery.store.query.building.DNFPredicate;
import tools.refinery.store.query.building.DNFPredicateCallAtom;
import tools.refinery.store.query.building.DirectRelationAtom;
import tools.refinery.store.query.building.Variable;

public class ParsedModelToDNFConverter {
	public Mappings transformPred(Set<PredicateDefinition> predicates, Map<Node, DNFNode> nodeMap,
			Map<tools.refinery.language.model.problem.Relation, Relation<TruthValue>> relationMap) throws Exception {
		Mappings mappings = new Mappings(new HashMap<>(), new HashMap<>(), nodeMap, relationMap);
		for (PredicateDefinition predicate : predicates) {
			if (predicate.getKind() == PredicateKind.DIRECT) {
				mappings.getPredicateMap().put(predicate, convertPredicateParameter(predicate, mappings));
			}
		}
		for (PredicateDefinition predicate : predicates) {
			if (predicate.getKind() == PredicateKind.DIRECT) {
				convertPredicateBody(predicate, mappings);
			}
		}
		return mappings;
	}

	protected DNFPredicate convertPredicateParameter(PredicateDefinition predicateDefinition, Mappings mappings)
			throws Exception {
		List<Variable> parameters = new ArrayList<>();
		for (Parameter parameter : predicateDefinition.getParameters()) {
			parameters.add(convertVariable(parameter, mappings));
		}
		return new DNFPredicate(predicateDefinition.getName(), parameters, new ArrayList<>());
	}

	protected void convertPredicateBody(PredicateDefinition predicateDefinition, Mappings mappings) throws Exception {
		List<DNFAnd> conjunctions = mappings.getPredicateMap().get(predicateDefinition).getClauses();
		for (Conjunction conjunction : predicateDefinition.getBodies()) {
			conjunctions.add(convertConjunction(conjunction, mappings));
		}
	}

	protected Variable convertVariable(tools.refinery.language.model.problem.Variable variable, Mappings mappings) {
		Variable dnfVar = mappings.getVariableMap().get(variable);
		if (dnfVar == null) {
			dnfVar = new Variable(variable.getName());
			mappings.getVariableMap().put(variable, dnfVar);
		}
		return dnfVar;
	}

	protected DNFNode convertNode(Node node, Mappings mappings) {
		return mappings.getNodeMap().get(node);
	}

	protected TruthValue convertLogicConstant(LogicConstant logicConstant) {
		return TruthValue.valueOf(logicConstant.getValue().toString());
	}

	protected Set<TruthValue> convertLogicConstants(Collection<LogicConstant> logicConstants, boolean refinement) {
		Set<TruthValue> truthValues = new HashSet<>();
		for (var logicConstant : logicConstants) {
			TruthValue converted = convertLogicConstant(logicConstant);
			truthValues.add(converted);
			if (refinement) {
				switch (converted) {
				case UNKNOWN:
					truthValues.add(TruthValue.ERROR);
					truthValues.add(TruthValue.TRUE);
					truthValues.add(TruthValue.FALSE);
					break;
				case TRUE, FALSE:
					truthValues.add(TruthValue.ERROR);
					break;
				case ERROR:
					break;
				default:
					throw new IllegalArgumentException("Unexpected value: " + converted);
				}
			}
		}
		return truthValues;
	}

	protected Variable convertArgument(Argument argument, Mappings mappings) throws Exception {
		if (argument instanceof VariableOrNodeArgument variableOrNodeArgument) {
			ImplicitVariable implicitVar = variableOrNodeArgument.getSingletonVariable();
			if (implicitVar == null) {
				VariableOrNode variableOrNode = variableOrNodeArgument.getVariableOrNode();
				if (variableOrNode instanceof tools.refinery.language.model.problem.Variable variable) {
					return convertVariable(variable, mappings);
				} else if (variableOrNode instanceof Node node) {
					throw new Exception("Node is not implemented yet");
				} else {
					throw new Exception("Unknown variableOrNodeArgument type");
				}
			} else {
				return convertVariable(implicitVar, mappings);
			}
		} else {
			throw new Exception("Unknown Argument type");
		}
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
			List<Variable> arguments = new ArrayList<>();
			for (Argument argument : atom.getArguments()) {
				arguments.add(convertArgument(argument, mappings));
			}
			Relation<TruthValue> relation = mappings.getRelationMap().get(atom.getRelation());
			Set<TruthValue> allowedTruthValues = convertLogicConstants(valueLiteral.getValues(),
					valueLiteral.isRefinement());
			return new DirectRelationAtom(relation, arguments, allowedTruthValues);
		} else if (literal instanceof Atom atom) {
			boolean transitive = atom.isTransitiveClosure();
			if (atom.getRelation()instanceof PredicateDefinition pred) {
				DNFPredicate dnfPredicate = mappings.getPredicateMap().get(pred);
				List<Variable> arguments = new ArrayList<>();
				for (Argument argument : atom.getArguments()) {
					arguments.add(convertArgument(argument, mappings));
				}
				return new DNFPredicateCallAtom(true, transitive, dnfPredicate, arguments);
			} else {
				throw new Exception("Unknown Relation type in Atom");
			}
		} else if (literal instanceof NegativeLiteral negativeLiteral) {
			Atom atom = negativeLiteral.getAtom();
			boolean transitive = atom.isTransitiveClosure();
			if (atom.getRelation()instanceof PredicateDefinition pred) {
				DNFPredicate dnfPredicate = mappings.getPredicateMap().get(pred);
				List<Variable> arguments = new ArrayList<>();
				for (Argument argument : atom.getArguments()) {
					arguments.add(convertArgument(argument, mappings));
				}
				return new DNFPredicateCallAtom(false, transitive, dnfPredicate, arguments);
			} else {
				throw new Exception("Unknown Relation type in Atom");
			}
		} else {
			throw new Exception("Unknown Literal type");
		}
	}
}
