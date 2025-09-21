/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.pquery;

import tools.refinery.interpreter.matchers.backend.IQueryBackendFactory;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.aggregations.BoundAggregator;
import tools.refinery.interpreter.matchers.psystem.annotations.PAnnotation;
import tools.refinery.interpreter.matchers.psystem.annotations.ParameterReference;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.*;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.Connectivity;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.*;
import tools.refinery.interpreter.matchers.psystem.queries.PParameter;
import tools.refinery.interpreter.matchers.psystem.queries.PParameterDirection;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.DnfClause;
import tools.refinery.logic.dnf.FunctionalDependency;
import tools.refinery.logic.dnf.SymbolicParameter;
import tools.refinery.logic.literal.*;
import tools.refinery.logic.term.*;
import tools.refinery.logic.util.CycleDetectingMapper;
import tools.refinery.store.query.view.AnySymbolView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Dnf2PQuery {
	private final CycleDetectingMapper<Dnf, RawPQuery> mapper = new CycleDetectingMapper<>(Dnf::name,
			this::doTranslate);
	private final QueryWrapperFactory wrapperFactory = new QueryWrapperFactory(this);
	private Function<Dnf, QueryEvaluationHint> computeHint = dnf -> new QueryEvaluationHint(null,
			(IQueryBackendFactory) null);

	public void setComputeHint(Function<Dnf, QueryEvaluationHint> computeHint) {
		this.computeHint = computeHint;
	}

	public RawPQuery translate(Dnf dnfQuery) {
		return mapper.map(dnfQuery);
	}

	public Map<AnySymbolView, IInputKey> getSymbolViews() {
		return wrapperFactory.getSymbolViews();
	}

	private RawPQuery doTranslate(Dnf dnfQuery) {
		var pQuery = new RawPQuery(dnfQuery.getUniqueName());
		pQuery.setEvaluationHints(computeHint.apply(dnfQuery));

		Map<SymbolicParameter, PParameter> parameters = new HashMap<>();
		List<PParameter> parameterList = new ArrayList<>();
		for (var parameter : dnfQuery.getSymbolicParameters()) {
			var direction = switch (parameter.getDirection()) {
				case OUT -> PParameterDirection.INOUT;
				case IN -> throw new IllegalArgumentException("Query %s with input parameter %s is not supported"
						.formatted(dnfQuery, parameter.getVariable()));
			};
			var pParameter = new PParameter(parameter.getVariable().getUniqueName(), null, null, direction);
			parameters.put(parameter, pParameter);
			parameterList.add(pParameter);
		}

		pQuery.setParameters(parameterList);

		for (var functionalDependency : dnfQuery.getFunctionalDependencies()) {
			var functionalDependencyAnnotation = getFunctionalDependencyAnnotation(functionalDependency);
			pQuery.addAnnotation(functionalDependencyAnnotation);
		}

		for (DnfClause clause : dnfQuery.getClauses()) {
			PBody body = new PBody(pQuery);
			List<ExportedParameter> parameterExports = new ArrayList<>();
			for (var parameter : dnfQuery.getSymbolicParameters()) {
				PVariable pVar = body.getOrCreateVariableByName(parameter.getVariable().getUniqueName());
				parameterExports.add(new ExportedParameter(body, pVar, parameters.get(parameter)));
			}
			body.setSymbolicParameters(parameterExports);
			pQuery.addBody(body);
			for (Literal literal : clause.literals()) {
				translateLiteral(clause, literal, body);
			}
		}

		return pQuery;
	}

	private static PAnnotation getFunctionalDependencyAnnotation(FunctionalDependency<Variable> functionalDependency) {
		var functionalDependencyAnnotation = new PAnnotation("FunctionalDependency");
		for (var forEachVariable : functionalDependency.forEach()) {
			var reference = new ParameterReference(forEachVariable.getUniqueName());
			functionalDependencyAnnotation.addAttribute("forEach", reference);
		}
		for (var uniqueVariable : functionalDependency.unique()) {
			var reference = new ParameterReference(uniqueVariable.getUniqueName());
			functionalDependencyAnnotation.addAttribute("unique", reference);
		}
		return functionalDependencyAnnotation;
	}

	private void translateLiteral(DnfClause clause, Literal literal, PBody body) {
		switch (literal) {
		case EquivalenceLiteral equivalenceLiteral -> translateEquivalenceLiteral(equivalenceLiteral, body);
		case CallLiteral callLiteral -> translateCallLiteral(callLiteral, body);
		case ConstantLiteral constantLiteral -> translateConstantLiteral(constantLiteral, body);
		case AssignLiteral<?> assignLiteral -> translateAssignLiteral(clause, assignLiteral, body);
		case CheckLiteral checkLiteral -> translateCheckLiteral(clause, checkLiteral, body);
		case RepresentativeElectionLiteral representativeElectionLiteral ->
				translateRepresentativeElectionLiteral(representativeElectionLiteral, body);
		case null, default -> throw new IllegalArgumentException("Unknown literal: " + literal);
		}
	}

	private void translateEquivalenceLiteral(EquivalenceLiteral equivalenceLiteral, PBody body) {
		PVariable varSource = body.getOrCreateVariableByName(equivalenceLiteral.getLeft().getUniqueName());
		PVariable varTarget = body.getOrCreateVariableByName(equivalenceLiteral.getRight().getUniqueName());
		if (equivalenceLiteral.isPositive()) {
			new Equality(body, varSource, varTarget);
		} else {
			new Inequality(body, varSource, varTarget);
		}
	}

	private void translateCallLiteral(CallLiteral callLiteral, PBody body) {
		var polarity = callLiteral.getPolarity();
		switch (polarity) {
		case POSITIVE -> {
			var substitution = translateSubstitution(callLiteral.getArguments(), body);
			var constraint = callLiteral.getTarget();
			switch (constraint) {
			case Dnf dnf -> {
				var pattern = translate(dnf);
				new PositivePatternCall(body, substitution, pattern);
			}
			case AnySymbolView symbolView -> {
				var inputKey = wrapperFactory.getInputKey(symbolView);
				new TypeConstraint(body, substitution, inputKey);
			}
			default -> throw new IllegalArgumentException("Unknown Constraint: " + constraint);
			}
		}
		case TRANSITIVE -> {
			var substitution = translateSubstitution(callLiteral.getArguments(), body);
			var pattern = wrapConstraintWithIdentityArguments(callLiteral.getTarget());
			new BinaryTransitiveClosure(body, substitution, pattern);
		}
		case NEGATIVE -> {
			var wrappedCall = wrapperFactory.maybeWrapConstraint(callLiteral);
			var substitution = translateSubstitution(wrappedCall.remappedArguments(), body);
			var pattern = wrappedCall.pattern();
			new NegativePatternCall(body, substitution, pattern);
		}
		default -> throw new IllegalArgumentException("Unknown polarity: " + polarity);
		}
	}

	private PQuery wrapConstraintWithIdentityArguments(Constraint constraint) {
		return switch (constraint) {
			case Dnf dnf -> translate(dnf);
			case AnySymbolView symbolView -> wrapperFactory.wrapSymbolViewIdentityArguments(symbolView);
			default -> throw new IllegalArgumentException("Unknown Constraint: " + constraint);
		};
	}

	private static Tuple translateSubstitution(List<Variable> substitution, PBody body) {
		int arity = substitution.size();
		Object[] variables = new Object[arity];
		for (int i = 0; i < arity; i++) {
			var variable = substitution.get(i);
			variables[i] = body.getOrCreateVariableByName(variable.getUniqueName());
		}
		return Tuples.flatTupleOf(variables);
	}

	private void translateConstantLiteral(ConstantLiteral constantLiteral, PBody body) {
		var variable = body.getOrCreateVariableByName(constantLiteral.getVariable().getUniqueName());
		new ConstantValue(body, variable, tools.refinery.store.tuple.Tuple.of(constantLiteral.getNodeId()));
	}

	private <T> void translateAssignLiteral(DnfClause clause, AssignLiteral<T> assignLiteral, PBody body) {
		var variable = body.getOrCreateVariableByName(assignLiteral.getVariable().getUniqueName());
		var term = assignLiteral.getTerm();
		if (term instanceof OptimizationBarrier<T>(var wrappedTerm)) {
			term = wrappedTerm;
		}
		switch (term) {
		case ConstantTerm<T> constantTerm -> new ConstantValue(body, variable, constantTerm.getValue());
		case CountTerm countTerm -> translateCountTerm(countTerm, variable, body);
		case AggregationTerm<T, ?> aggregationTerm -> translateAggregationTerm(aggregationTerm, variable, body);
		case LeftJoinTerm<T> leftJoinTerm -> translateLeftJoinTerm(leftJoinTerm, variable, body);
		default -> {
			var evaluator = new TermEvaluator<>(term, clause);
			new ExpressionEvaluation(body, evaluator, variable);
		}
		}
	}

	private void translateCheckLiteral(DnfClause clause, CheckLiteral checkLiteral, PBody body) {
		var evaluator = new CheckEvaluator(checkLiteral.getTerm(), clause);
		new ExpressionEvaluation(body, evaluator, null);
	}

	private void translateCountTerm(CountTerm countLiteral, PVariable resultVariable, PBody body) {
		var wrappedCall = wrapperFactory.maybeWrapConstraint(countLiteral);
		var substitution = translateSubstitution(wrappedCall.remappedArguments(), body);
		new PatternMatchCounter(body, substitution, wrappedCall.pattern(), resultVariable);
	}

	private <R, T> void translateAggregationTerm(AggregationTerm<R, T> aggregationTerm, PVariable resultVariable,
												 PBody body) {
		var aggregator = aggregationTerm.getAggregator();
		var aggregationOperator = switch (aggregator) {
			case StatelessAggregator<R, T> statelessAggregator ->
					new StatelessMultisetAggregator<>(statelessAggregator);
			case StatefulAggregator<R, T> statefulAggregator -> new StatefulMultisetAggregator<>(statefulAggregator);
			default -> throw new IllegalArgumentException("Unknown aggregator: " + aggregator);
		};
		var wrappedCall = wrapperFactory.maybeWrapConstraint(aggregationTerm);
		var substitution = translateSubstitution(wrappedCall.remappedArguments(), body);
		var inputVariable = body.getOrCreateVariableByName(aggregationTerm.getInputVariable().getUniqueName());
		var aggregatedColumn = substitution.invertIndex().get(inputVariable);
		if (aggregatedColumn == null) {
			throw new IllegalStateException("Input variable %s not found in substitution %s".formatted(inputVariable,
					substitution));
		}
		var boundAggregator = new BoundAggregator(aggregationOperator, aggregator.getInputType(),
				aggregator.getResultType());
		new AggregatorConstraint(boundAggregator, body, substitution, wrappedCall.pattern(), resultVariable,
				aggregatedColumn);
	}

	private <T> void translateLeftJoinTerm(LeftJoinTerm<T> leftJoinTerm, PVariable resultVariable, PBody body) {
		var wrappedCall = wrapperFactory.maybeWrapConstraint(leftJoinTerm);
		var substitution = translateSubstitution(wrappedCall.remappedArguments(), body);
		var placeholderVariable = body.getOrCreateVariableByName(
				leftJoinTerm.getPlaceholderVariable().getUniqueName());
		var optionalColumn = substitution.invertIndex().get(placeholderVariable);
		if (optionalColumn == null) {
			throw new IllegalStateException("Placeholder variable %s not found in substitution %s"
					.formatted(placeholderVariable, substitution));
		}
		new LeftJoinConstraint(body, substitution, wrappedCall.pattern(), resultVariable, optionalColumn,
				leftJoinTerm.getDefaultValue());
	}

	private void translateRepresentativeElectionLiteral(RepresentativeElectionLiteral literal, PBody body) {
		var substitution = translateSubstitution(literal.getArguments(), body);
		var pattern = wrapConstraintWithIdentityArguments(literal.getTarget());
		var connectivity = switch (literal.getConnectivity()) {
			case WEAK -> Connectivity.WEAK;
			case STRONG -> Connectivity.STRONG;
		};
		new RepresentativeElectionConstraint(body, substitution, pattern, connectivity);
	}
}
