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
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.literal.CallPolarity;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.AnyTerm;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.transition.DecisionRule;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.RuleBuilder;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.dse.transition.actions.ActionLiterals;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.*;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;

import java.util.*;

public class RuleCompiler {
	private static final String UNKNOWN_ACTION_MESSAGE = "Unknown action";
	static final String INVALID_ARGUMENT_MESSAGE = "Invalid argument";

	@Inject
	private SemanticsUtils semanticsUtils;

	@Inject
	private BuiltinAnnotationContext builtinAnnotationContext;

	private QueryCompiler queryCompiler;

	public void setQueryCompiler(QueryCompiler queryCompiler) {
		this.queryCompiler = queryCompiler;
	}

	public DecisionRule toDecisionRule(String name, RuleDefinition ruleDefinition) {
		var rule = toDecisionRuleInternal(name, ruleDefinition);
		var settings = builtinAnnotationContext.getDecisionSettings(ruleDefinition);
		return new DecisionRule(rule, settings.priority(), settings.coefficient(), settings.exponent());
	}

	private Rule toDecisionRuleInternal(String name, RuleDefinition ruleDefinition) {
		var consequents = ruleDefinition.getConsequents();
		if (consequents.isEmpty()) {
			return toRule(name, ruleDefinition);
		}
		var preparedRule = prepareRule(ruleDefinition, true);
		var firstConsequent = consequents.getFirst();
		var wrappedActions = wrapConsequent(firstConsequent, preparedRule);
		var parameters = preparedRule.allParameters();

		var moreCommonLiterals = new ArrayList<Literal>();
		var delayedActions = new ArrayList<WrappedAction>();
		for (var action : wrappedActions) {
			if (action.toLiterals(true, ConcreteModality.PARTIAL_MAY, moreCommonLiterals)) {
				delayedActions.add(action);
			}
		}
		var precondition = preparedRule.buildQuery(name, parameters, moreCommonLiterals, queryCompiler);
		for (var action : wrappedActions) {
			action.setPrecondition(precondition);
		}

		var blockerLiterals = new ArrayList<Literal>(moreCommonLiterals.size() + 1);
		blockerLiterals.add(PartialLiterals.must(precondition.call(CallPolarity.POSITIVE, parameters)));
		toLiteralsChecked(wrappedActions, ConcreteModality.CANDIDATE_MUST, blockerLiterals);
		var blocker = Query.builder(name + "#blocker")
				.parameters(parameters)
				.clause(blockerLiterals)
				.build();

		var withBlockerLiterals = new ArrayList<Literal>(delayedActions.size() + 2);
		withBlockerLiterals.add(PartialLiterals.must(precondition.call(CallPolarity.POSITIVE, parameters)));
		withBlockerLiterals.add(blocker.call(CallPolarity.NEGATIVE, parameters));
		toLiteralsChecked(delayedActions, ConcreteModality.PARTIAL_MAY, withBlockerLiterals);
		var preconditionWithBlocker = Query.builder(name + "#withBlocker")
				.parameters(parameters)
				.clause(withBlockerLiterals)
				.build();

		var ruleBuilder = Rule.builder(name)
				.parameters(parameters)
				.clause(preconditionWithBlocker.call(CallPolarity.POSITIVE, parameters));
		buildConsequent(firstConsequent, wrappedActions, preparedRule, ruleBuilder);
		return ruleBuilder.build();
	}

	private List<WrappedAction> wrapConsequent(Consequent consequent, PreparedRule preparedRule) {
		return consequent.getActions().stream()
				.map(action -> wrapAction(action, preparedRule))
				.toList();
	}

	private WrappedAction wrapAction(Action action, PreparedRule preparedRule) {
		if (!(action instanceof AssertionAction assertionAction)) {
			throw new TracedException(action, UNKNOWN_ACTION_MESSAGE);
		}
		var literals = new ArrayList<Literal>();
		var term = queryCompiler.interpretTerm(assertionAction.getValue(), preparedRule.parameterMap(), literals);
		var partialSymbol = queryCompiler.getPartialSymbol(assertionAction.getRelation());
		var arguments = assertionAction.getArguments();
		return switch (partialSymbol) {
			case PartialRelation partialRelation -> {
				var truthValueTerm = term.asType(TruthValue.class);
				if (truthValueTerm instanceof ConstantTerm<TruthValue> constantTerm && literals.isEmpty()) {
					yield new WrappedRelationAction(this, preparedRule, partialRelation, arguments,
							constantTerm.getValue());
				}
				yield new WrappedComputedRelationAction(this, preparedRule, partialRelation, arguments, literals,
                        truthValueTerm);
			}
			case PartialFunction<?, ?> partialFunction -> wrapAction(arguments, literals, term, partialFunction,
					preparedRule);
		};
	}

	private <A extends AbstractValue<A, C>, C> WrappedAction wrapAction(
			List<AssertionArgument> arguments, List<Literal> literals, AnyTerm anyTerm,
			PartialFunction<A, C> partialFunction, PreparedRule preparedRule) {
		var term = anyTerm.asType(partialFunction.abstractDomain().abstractType());
		if (term instanceof ConstantTerm<A> constantTerm && literals.isEmpty()) {
			return new WrappedConstantFunctionAction<>(this, preparedRule, partialFunction,
					arguments, constantTerm.getValue());
		}
		return new WrappedComputedFunctionAction<>(this, preparedRule, partialFunction,
				arguments, literals, term);
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
			try {
				var wrappedAction = wrapAction(action, preparedRule);
				var parameters = wrappedAction.getNodeVariables();
				var moreCommonLiterals = new ArrayList<Literal>();
				boolean delayed = wrappedAction.toLiterals(false, postConditionModality, moreCommonLiterals);
				var omittedParametersMustExist = concreteness == ConcretenessSpecification.CANDIDATE;
				var precondition = preparedRule.buildQuery(actionName, parameters, moreCommonLiterals, queryCompiler,
						omittedParametersMustExist);
				wrappedAction.setPrecondition(precondition);
				if (delayed) {
					var literalsWithDelayed = new ArrayList<Literal>(2);
					literalsWithDelayed.add(precondition.call(CallPolarity.POSITIVE, parameters));
					wrappedAction.toLiteralsChecked(false, postConditionModality, literalsWithDelayed);
					precondition = Query.builder(precondition.name() + "#withDelayed")
							.symbolicParameters(precondition.getDnf().getSymbolicParameters())
							.clause(literalsWithDelayed)
							.build();
				}
				var actionLiterals = new ArrayList<ActionLiteral>();
				wrappedAction.toActionLiterals(concreteness.toConcreteness(), actionLiterals,
						preparedRule.parameterMap());
				var rule = Rule.builder(actionName)
						.parameters(parameters)
						.clause(new ModalConstraint(ModalitySpecification.MUST, concreteness, precondition.getDnf())
								.call(CallPolarity.POSITIVE, parameters))
						.action(actionLiterals)
						.build();
				rules.add(rule);
			} catch (RuntimeException e) {
				throw TracedException.addTrace(action, e);
			}
		}
		return rules;
	}

	public Rule toRule(String name, RuleDefinition ruleDefinition) {
		var preparedRule = prepareRule(ruleDefinition, true);
		var parameters = preparedRule.allParameters();
		var precondition = preparedRule.buildQuery(name, parameters, List.of(), queryCompiler);
		var ruleBuilder = Rule.builder(name)
				.parameters(parameters)
				.clause(PartialLiterals.must(precondition.call(CallPolarity.POSITIVE, parameters)));
		for (var consequent : ruleDefinition.getConsequents()) {
			var wrappedActions = wrapConsequent(consequent, preparedRule);
			for (var action : wrappedActions) {
				action.setPrecondition(precondition);
			}
			buildConsequent(consequent, wrappedActions, preparedRule, ruleBuilder);
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

	private void buildConsequent(Consequent body, List<WrappedAction> wrappedActions, PreparedRule preparedRule,
								 RuleBuilder builder) {
		try {
			var actionLiterals = new ArrayList<ActionLiteral>();
			var localScope = preparedRule.focusParameters(actionLiterals);
			for (var action : wrappedActions) {
				action.toActionLiterals(Concreteness.PARTIAL, actionLiterals, localScope);
			}
			builder.action(actionLiterals);
		} catch (RuntimeException e) {
			throw TracedException.addTrace(body, e);
		}
	}

	@NotNull String getTempVariableName(Node node, int nodeId) {
		return semanticsUtils.getNameWithoutRootPrefix(node).orElse("_" + nodeId);
	}

	private void toLiteralsChecked(List<WrappedAction> wrappedActions, ConcreteModality concreteModality,
								   List<Literal> literals) {
		for (var action : wrappedActions) {
			action.toLiteralsChecked(true, concreteModality, literals);
		}
	}

	NodeVariable[] collectArguments(List<AssertionArgument> problemArguments, PreparedRule preparedRule,
									ConcreteModality concreteModality, List<Literal> literals) {
		var arguments = new NodeVariable[problemArguments.size()];
		var referenceCounts = ReferenceCounter.computeReferenceCounts(problemArguments);
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

	NodeVariable[] collectArguments(List<AssertionArgument> problemArguments,
									Map<tools.refinery.language.model.problem.Variable, NodeVariable> localScope,
									List<ActionLiteral> actionLiterals) {
		var arguments = new NodeVariable[problemArguments.size()];
		for (int i = 0; i < arguments.length; i++) {
			var problemArgument = problemArguments.get(i);
			if (!(problemArgument instanceof NodeAssertionArgument nodeAssertionArgument)) {
				throw new TracedException(problemArgument, RuleCompiler.INVALID_ARGUMENT_MESSAGE);
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
			default -> throw new TracedException(problemArgument, RuleCompiler.INVALID_ARGUMENT_MESSAGE);
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
