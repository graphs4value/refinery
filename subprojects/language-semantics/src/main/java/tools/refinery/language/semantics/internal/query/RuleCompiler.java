/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.semantics.SemanticsUtils;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.language.utils.BuiltinAnnotationContext;
import tools.refinery.language.utils.ParameterBinding;
import tools.refinery.language.validation.ReferenceCounter;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.literal.BooleanLiteral;
import tools.refinery.logic.literal.CallPolarity;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.RuleBuilder;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.dse.transition.actions.ActionLiterals;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.ModalitySpecification;
import tools.refinery.store.reasoning.literal.PartialLiterals;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;

import java.util.*;

public class RuleCompiler {
	private static final String UNKNOWN_ACTION_MESSAGE = "Unknown action";
	private static final String INVALID_ARGUMENT_MESSAGE = "Invalid argument";

	@Inject
	private SemanticsUtils semanticsUtils;

	@Inject
	private BuiltinAnnotationContext builtinAnnotationContext;

	private QueryCompiler queryCompiler;

	public void setQueryCompiler(QueryCompiler queryCompiler) {
		this.queryCompiler = queryCompiler;
	}

	public Rule toDecisionRule(String name, RuleDefinition ruleDefinition) {
		var consequents = ruleDefinition.getConsequents();
		if (consequents.isEmpty()) {
			return toRule(name, ruleDefinition);
		}
		var firstConsequent = consequents.getFirst();
		var preparedRule = prepareRule(ruleDefinition, true);
		var parameters = preparedRule.allParameters();

		var moreCommonLiterals = new ArrayList<Literal>();
		toLiterals(firstConsequent, preparedRule, ConcreteModality.PARTIAL_MAY, moreCommonLiterals);
		var precondition = preparedRule.buildQuery(name, parameters, moreCommonLiterals, queryCompiler);

		var blockerLiterals = new ArrayList<Literal>(moreCommonLiterals.size() + 1);
		blockerLiterals.add(PartialLiterals.must(precondition.call(CallPolarity.POSITIVE, parameters)));
		toLiterals(firstConsequent, preparedRule, ConcreteModality.CANDIDATE_MUST, blockerLiterals);
		var blocker = Query.builder(name + "#blocker")
				.parameters(parameters)
				.clause(blockerLiterals)
				.build();

		var preconditionWithBlocker = Query.builder(name + "#withBlocker")
				.parameters(parameters)
				.clause(
						PartialLiterals.must(precondition.call(CallPolarity.POSITIVE, parameters)),
						blocker.call(CallPolarity.NEGATIVE, parameters)
				)
				.build();

		var ruleBuilder = Rule.builder(name)
				.parameters(parameters)
				.clause(preconditionWithBlocker.call(CallPolarity.POSITIVE, parameters));
		for (var consequent : ruleDefinition.getConsequents()) {
			buildConsequent(consequent, preparedRule, ruleBuilder);
		}
		return ruleBuilder.build();
	}

	public Collection<Rule> toPropagationRules(String name, RuleDefinition ruleDefinition,
											   ConcretenessSpecification concreteness) {
		var preparedRule = prepareRule(ruleDefinition, false);
		if (preparedRule.hasFocus()) {
			throw new IllegalArgumentException("Propagation rule '%s' must not have any focus parameters."
					.formatted(name));
		}
		var consequents = ruleDefinition.getConsequents();
		if (consequents.isEmpty()) {
			return List.of();
		}
		if (consequents.size() > 1) {
			throw new IllegalArgumentException("Propagation rule '%s' should have a single consequent."
					.formatted(name));
		}
		var actions = consequents.getFirst().getActions();
		int actionCount = actions.size();
		var rules = new ArrayList<Rule>(actionCount);
		var postConditionModality = new ConcreteModality(concreteness, ModalitySpecification.MAY);
		for (int i = 0; i < actionCount; i++) {
			var actionName = actionCount == 1 ? name : name + "#" + (i + 1);
			var action = actions.get(i);
			if (!(action instanceof AssertionAction assertionAction)) {
				throw new TracedException(action, UNKNOWN_ACTION_MESSAGE);
			}
			try {
				var parameters = getParameterList(assertionAction, preparedRule);
				var moreCommonLiterals = new ArrayList<Literal>();
				toLiterals(assertionAction, preparedRule, false, postConditionModality, moreCommonLiterals);
				var omittedParametersMustExist = concreteness == ConcretenessSpecification.CANDIDATE;
				var precondition = preparedRule.buildQuery(actionName, parameters, moreCommonLiterals, queryCompiler,
						omittedParametersMustExist);
				var actionLiterals = new ArrayList<ActionLiteral>();
				toActionLiterals(action, preparedRule.parameterMap(), actionLiterals);
				var rule = Rule.builder(actionName)
						.parameters(parameters)
						.clause(new ModalConstraint(ModalitySpecification.MUST, concreteness, precondition.getDnf())
								.call(CallPolarity.POSITIVE, Collections.unmodifiableList(parameters)))
						.action(actionLiterals)
						.build();
				rules.add(rule);
			} catch (RuntimeException e) {
				throw TracedException.addTrace(action, e);
			}
		}
		return rules;
	}

	private static List<NodeVariable> getParameterList(AssertionAction assertionAction, PreparedRule preparedRule) {
		var parameterSet = new LinkedHashSet<NodeVariable>();
		for (AssertionArgument argument : assertionAction.getArguments()) {
			if (argument instanceof NodeAssertionArgument nodeAssertionArgument) {
				VariableOrNode variableOrNode = nodeAssertionArgument.getNode();
				if (variableOrNode instanceof tools.refinery.language.model.problem.Variable problemVariable) {
					NodeVariable nodeVariable = preparedRule.parameterMap().get(problemVariable);
					parameterSet.add(nodeVariable);
				}
			}
		}
		return List.copyOf(parameterSet);
	}

	public Rule toRule(String name, RuleDefinition ruleDefinition) {
		var preparedRule = prepareRule(ruleDefinition, true);
		var parameters = preparedRule.allParameters();
		var precondition = preparedRule.buildQuery(name, parameters, List.of(), queryCompiler);
		var ruleBuilder = Rule.builder(name)
				.parameters(parameters)
				.clause(PartialLiterals.must(precondition.call(CallPolarity.POSITIVE, parameters)));
		for (var consequent : ruleDefinition.getConsequents()) {
			buildConsequent(consequent, preparedRule, ruleBuilder);
		}
		return ruleBuilder.build();
	}

	private PreparedRule prepareRule(RuleDefinition ruleDefinition, boolean needsExplicitMultiObjectParameters) {
		var problemParameters = ruleDefinition.getParameters();
		int arity = problemParameters.size();
		var parameterMap = LinkedHashMap
				.<tools.refinery.language.model.problem.Variable, NodeVariable>newLinkedHashMap(arity);
		var commonLiterals = new ArrayList<Literal>();
		var parametersToFocus = new ArrayList<tools.refinery.language.model.problem.Variable>();
		for (var problemParameter : problemParameters) {
			var parameter = Variable.of(problemParameter.getName());
			parameterMap.put(problemParameter, parameter);
			var parameterType = problemParameter.getParameterType();
			if (parameterType != null) {
				var partialType = getPartialRelation(parameterType);
				commonLiterals.add(partialType.call(parameter));
			}
			var binding = builtinAnnotationContext.getParameterBinding(problemParameter);
			if (needsExplicitMultiObjectParameters) {
				if (binding == ParameterBinding.SINGLE) {
					commonLiterals.add(MultiObjectTranslator.MULTI_VIEW.call(CallPolarity.NEGATIVE, parameter));
				} else if (binding == ParameterBinding.LONE) {
					commonLiterals.add(new ModalConstraint(ModalitySpecification.MUST,
							ConcretenessSpecification.UNSPECIFIED, ReasoningAdapter.EQUALS_SYMBOL)
							.call(parameter, parameter));
				}
			}
			if (binding == ParameterBinding.FOCUS) {
				parametersToFocus.add(problemParameter);
			}
		}
		PreparedRule.toMonomorphicMatchingLiterals(parametersToFocus, parameterMap, commonLiterals);
		return new PreparedRule(ruleDefinition, Collections.unmodifiableSequencedMap(parameterMap),
				Collections.unmodifiableCollection(parametersToFocus), Collections.unmodifiableList(commonLiterals));
	}

	private void buildConsequent(Consequent body, PreparedRule preparedRule, RuleBuilder builder) {
		try {
			var actionLiterals = new ArrayList<ActionLiteral>();
			var localScope = preparedRule.focusParameters(actionLiterals);
			for (var action : body.getActions()) {
				toActionLiterals(action, localScope, actionLiterals);
			}
			builder.action(actionLiterals);
		} catch (RuntimeException e) {
			throw TracedException.addTrace(body, e);
		}
	}

	private void toActionLiterals(
			Action action, Map<tools.refinery.language.model.problem.Variable, NodeVariable> localScope,
			List<ActionLiteral> actionLiterals) {
		if (!(action instanceof AssertionAction assertionAction)) {
			throw new TracedException(action, UNKNOWN_ACTION_MESSAGE);
		}
		var partialRelation = getPartialRelation(assertionAction.getRelation());
		var truthValue = SemanticsUtils.getTruthValue(assertionAction.getValue());
		var problemArguments = assertionAction.getArguments();
		var arguments = new NodeVariable[problemArguments.size()];
		for (int i = 0; i < arguments.length; i++) {
			var problemArgument = problemArguments.get(i);
			if (!(problemArgument instanceof NodeAssertionArgument nodeAssertionArgument)) {
				throw new TracedException(problemArgument, INVALID_ARGUMENT_MESSAGE);
			}
			var variableOrNode = nodeAssertionArgument.getNode();
			switch (variableOrNode) {
			case tools.refinery.language.model.problem.Variable problemVariable ->
					arguments[i] = localScope.get(problemVariable);
			case Node node -> {
				int nodeId = getNodeId(node);
				var tempVariable = Variable.of(getTempVariableName(node, nodeId));
				actionLiterals.add(ActionLiterals.constant(tempVariable, nodeId));
				arguments[i] = tempVariable;
			}
			default -> throw new TracedException(problemArgument, INVALID_ARGUMENT_MESSAGE);
			}
		}
		actionLiterals.add(PartialActionLiterals.merge(partialRelation, truthValue, arguments));
	}

	private @NotNull String getTempVariableName(Node node, int nodeId) {
		return semanticsUtils.getNameWithoutRootPrefix(node).orElse("_" + nodeId);
	}

	private void toLiterals(Consequent body, PreparedRule preparedRule, ConcreteModality concreteModality,
							List<Literal> literals) {
		for (var action : body.getActions()) {
			if (!(action instanceof AssertionAction assertionAction)) {
				throw new TracedException(action, UNKNOWN_ACTION_MESSAGE);
			}
			toLiterals(assertionAction, preparedRule, true, concreteModality, literals);
		}
	}

	private void toLiterals(AssertionAction assertionAction, PreparedRule preparedRule, boolean positive,
							ConcreteModality concreteModality, List<Literal> literals) {
		var truthValue = SemanticsUtils.getTruthValue(assertionAction.getValue());
		if (truthValue == TruthValue.UNKNOWN) {
			if (!positive) {
				literals.add(BooleanLiteral.FALSE);
			}
			return;
		}
		var partialRelation = getPartialRelation(assertionAction.getRelation());
		var arguments = collectArguments(assertionAction, preparedRule, concreteModality, literals);
		if (truthValue == TruthValue.ERROR ||
				(truthValue == TruthValue.TRUE && positive) ||
				(truthValue == TruthValue.FALSE && !positive)) {
			literals.add(concreteModality.wrapConstraint(partialRelation).call(CallPolarity.POSITIVE, arguments));
		}
		if (truthValue == TruthValue.ERROR ||
				(truthValue == TruthValue.FALSE && positive) ||
				(truthValue == TruthValue.TRUE && !positive)) {
			literals.add(concreteModality.negate().wrapConstraint(partialRelation)
					.call(CallPolarity.NEGATIVE, arguments));
		}
	}

	private NodeVariable[] collectArguments(AssertionAction assertionAction, PreparedRule preparedRule,
											ConcreteModality concreteModality, List<Literal> literals) {
		var problemArguments = assertionAction.getArguments();
		var arguments = new NodeVariable[problemArguments.size()];
		var referenceCounts = ReferenceCounter.computeReferenceCounts(assertionAction);
		for (int i = 0; i < arguments.length; i++) {
			var problemArgument = problemArguments.get(i);
			if (!(problemArgument instanceof NodeAssertionArgument nodeAssertionArgument)) {
				throw new TracedException(problemArgument, INVALID_ARGUMENT_MESSAGE);
			}
			var variableOrNode = nodeAssertionArgument.getNode();
			switch (variableOrNode) {
			case tools.refinery.language.model.problem.Variable problemVariable -> {
				var argument = preparedRule.parameterMap().get(problemVariable);
				arguments[i] = argument;
				if (referenceCounts.getOrDefault(problemVariable, 0) >= 2) {
					// Diagonal assertions can't be represented for multi-objects.
					literals.add(new ModalConstraint(ModalitySpecification.MUST, concreteModality.concreteness(),
							ReasoningAdapter.EQUALS_SYMBOL).call(argument, argument));
				}
			}
			case Node node -> {
				int nodeId = getNodeId(node);
				var tempVariable = Variable.of(getTempVariableName(node, nodeId));
				literals.add(tempVariable.isConstant(nodeId));
				arguments[i] = tempVariable;
			}
			default -> throw new TracedException(problemArgument, INVALID_ARGUMENT_MESSAGE);
			}
		}
		return arguments;
	}

	private int getNodeId(Node node) {
		return queryCompiler.getNodeId(node);
	}

	private PartialRelation getPartialRelation(Relation relation) {
		return queryCompiler.getPartialRelation(relation);
	}
}
