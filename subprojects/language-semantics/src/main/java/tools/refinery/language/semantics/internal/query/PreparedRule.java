/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import tools.refinery.language.model.problem.RuleDefinition;
import tools.refinery.language.model.problem.Variable;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.literal.Literals;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.literal.PartialLiterals;

import java.util.*;

record PreparedRule(
		RuleDefinition ruleDefinition, SequencedMap<Variable, NodeVariable> parameterMap,
		Collection<Variable> parametersToFocus, List<Literal> commonLiterals) {
	public List<NodeVariable> allParameters() {
		return List.copyOf(parameterMap.sequencedValues());
	}

	public RelationalQuery buildQuery(String name, List<NodeVariable> parameters, List<Literal> moreCommonLiterals,
									  QueryCompiler queryCompiler) {
		return buildQuery(name, parameters, moreCommonLiterals, queryCompiler, false);
	}

	public RelationalQuery buildQuery(String name, List<NodeVariable> parameters, List<Literal> moreCommonLiterals,
									  QueryCompiler queryCompiler, boolean omittedParametersMustExist) {
		var allCommonLiterals = new ArrayList<>(commonLiterals);
		for (var parameter : parameterMap.values()) {
			// Make sure to always existentially quantify all omitted parameters.
			if (omittedParametersMustExist || !parameters.contains(parameter)) {
				allCommonLiterals.add(ReasoningAdapter.EXISTS_SYMBOL.call(parameter));
			}
		}
		allCommonLiterals.addAll(moreCommonLiterals);
		var queryBuilder = Query.builder(name).parameters(parameters);
		var preconditions = ruleDefinition.getPreconditions();
		if (preconditions.isEmpty()) {
			queryBuilder.clause(allCommonLiterals);
		} else {
			for (var precondition : preconditions) {
				queryCompiler.buildConjunction(precondition, parameterMap, allCommonLiterals, queryBuilder);
			}
		}
		return queryBuilder.build();
	}

	public boolean hasFocus() {
		return !parametersToFocus.isEmpty();
	}

	public Map<Variable, NodeVariable> focusParameters(List<ActionLiteral> actionLiterals) {
		if (!hasFocus()) {
			return parameterMap;
		}
		var localScope = new LinkedHashMap<>(parameterMap);
		for (var parameterToFocus : parametersToFocus) {
			var originalParameter = parameterMap.get(parameterToFocus);
			var focusedParameter = tools.refinery.logic.term.Variable.of(originalParameter.getName() + "#focused");
			localScope.put(parameterToFocus, focusedParameter);
			actionLiterals.add(PartialActionLiterals.focus(originalParameter, focusedParameter));
		}
		return Collections.unmodifiableMap(localScope);
	}

	public static void toMonomorphicMatchingLiterals(
			List<tools.refinery.language.model.problem.Variable> parametersToFocus,
			Map<tools.refinery.language.model.problem.Variable, NodeVariable> parameterMap,
			List<Literal> commonLiterals) {
		int focusCount = parametersToFocus.size();
		for (int i = 0; i < focusCount; i++) {
			var leftFocus = parameterMap.get(parametersToFocus.get(i));
			for (int j = i + 1; j < focusCount; j++) {
				var rightFocus = parameterMap.get(parametersToFocus.get(j));
				commonLiterals.add(Literals.not(PartialLiterals.must(
						ReasoningAdapter.EQUALS_SYMBOL.call(leftFocus, rightFocus))));
			}
		}
	}
}
