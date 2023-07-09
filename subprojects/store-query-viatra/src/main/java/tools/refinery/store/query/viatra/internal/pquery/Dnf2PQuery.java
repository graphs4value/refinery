/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.viatra.internal.pquery;

import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackendFactory;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import org.eclipse.viatra.query.runtime.matchers.context.IInputKey;
import org.eclipse.viatra.query.runtime.matchers.psystem.PBody;
import org.eclipse.viatra.query.runtime.matchers.psystem.PVariable;
import org.eclipse.viatra.query.runtime.matchers.psystem.aggregations.BoundAggregator;
import org.eclipse.viatra.query.runtime.matchers.psystem.aggregations.IMultisetAggregationOperator;
import org.eclipse.viatra.query.runtime.matchers.psystem.annotations.PAnnotation;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicdeferred.*;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.BinaryTransitiveClosure;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.ConstantValue;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.PositivePatternCall;
import org.eclipse.viatra.query.runtime.matchers.psystem.basicenumerables.TypeConstraint;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PParameter;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PParameterDirection;
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.DnfClause;
import tools.refinery.store.query.dnf.SymbolicParameter;
import tools.refinery.store.query.literal.*;
import tools.refinery.store.query.term.ConstantTerm;
import tools.refinery.store.query.term.StatefulAggregator;
import tools.refinery.store.query.term.StatelessAggregator;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.util.CycleDetectingMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Dnf2PQuery {
	private static final Object P_CONSTRAINT_LOCK = new Object();
	private final CycleDetectingMapper<Dnf, RawPQuery> mapper = new CycleDetectingMapper<>(Dnf::name,
			this::doTranslate);
	private final QueryWrapperFactory wrapperFactory = new QueryWrapperFactory(this);
	private final Map<Dnf, QueryEvaluationHint> hintOverrides = new LinkedHashMap<>();
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

	public void hint(Dnf dnf, QueryEvaluationHint hint) {
		hintOverrides.compute(dnf, (ignoredKey, existingHint) ->
				existingHint == null ? hint : existingHint.overrideBy(hint));
	}

	private QueryEvaluationHint consumeHint(Dnf dnf) {
		var defaultHint = computeHint.apply(dnf);
		var existingHint = hintOverrides.remove(dnf);
		return defaultHint.overrideBy(existingHint);
	}

	public void assertNoUnusedHints() {
		if (hintOverrides.isEmpty()) {
			return;
		}
		var unusedHints = hintOverrides.keySet().stream().map(Dnf::name).collect(Collectors.joining(", "));
		throw new IllegalStateException(
				"Unused query evaluation hints for %s. Hints must be set before a query is added to the engine"
						.formatted(unusedHints));
	}

	private RawPQuery doTranslate(Dnf dnfQuery) {
		var pQuery = new RawPQuery(dnfQuery.getUniqueName());
		pQuery.setEvaluationHints(consumeHint(dnfQuery));

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
			var functionalDependencyAnnotation = new PAnnotation("FunctionalDependency");
			for (var forEachVariable : functionalDependency.forEach()) {
				functionalDependencyAnnotation.addAttribute("forEach", forEachVariable.getUniqueName());
			}
			for (var uniqueVariable : functionalDependency.unique()) {
				functionalDependencyAnnotation.addAttribute("unique", uniqueVariable.getUniqueName());
			}
			pQuery.addAnnotation(functionalDependencyAnnotation);
		}

		// The constructor of {@link org.eclipse.viatra.query.runtime.matchers.psystem.BasePConstraint} mutates
		// global static state (<code>nextID</code>) without locking. Therefore, we need to synchronize before creating
		// any query literals to avoid a data race.
		synchronized (P_CONSTRAINT_LOCK) {
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
					translateLiteral(literal, body);
				}
			}
		}

		return pQuery;
	}

	private void translateLiteral(Literal literal, PBody body) {
		if (literal instanceof EquivalenceLiteral equivalenceLiteral) {
			translateEquivalenceLiteral(equivalenceLiteral, body);
		} else if (literal instanceof CallLiteral callLiteral) {
			translateCallLiteral(callLiteral, body);
		} else if (literal instanceof ConstantLiteral constantLiteral) {
			translateConstantLiteral(constantLiteral, body);
		} else if (literal instanceof AssignLiteral<?> assignLiteral) {
			translateAssignLiteral(assignLiteral, body);
		} else if (literal instanceof CheckLiteral checkLiteral) {
			translateCheckLiteral(checkLiteral, body);
		} else if (literal instanceof CountLiteral countLiteral) {
			translateCountLiteral(countLiteral, body);
		} else if (literal instanceof AggregationLiteral<?, ?> aggregationLiteral) {
			translateAggregationLiteral(aggregationLiteral, body);
		} else {
			throw new IllegalArgumentException("Unknown literal: " + literal.toString());
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
			if (constraint instanceof Dnf dnf) {
				var pattern = translate(dnf);
				new PositivePatternCall(body, substitution, pattern);
			} else if (constraint instanceof AnySymbolView symbolView) {
				var inputKey = wrapperFactory.getInputKey(symbolView);
				new TypeConstraint(body, substitution, inputKey);
			} else {
				throw new IllegalArgumentException("Unknown Constraint: " + constraint);
			}
		}
		case TRANSITIVE -> {
			var substitution = translateSubstitution(callLiteral.getArguments(), body);
			var constraint = callLiteral.getTarget();
			PQuery pattern;
			if (constraint instanceof Dnf dnf) {
				pattern = translate(dnf);
			} else if (constraint instanceof AnySymbolView symbolView) {
				pattern = wrapperFactory.wrapSymbolViewIdentityArguments(symbolView);
			} else {
				throw new IllegalArgumentException("Unknown Constraint: " + constraint);
			}
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
		new ConstantValue(body, variable, constantLiteral.getNodeId());
	}

	private <T> void translateAssignLiteral(AssignLiteral<T> assignLiteral, PBody body) {
		var variable = body.getOrCreateVariableByName(assignLiteral.getVariable().getUniqueName());
		var term = assignLiteral.getTerm();
		if (term instanceof ConstantTerm<T> constantTerm) {
			new ConstantValue(body, variable, constantTerm.getValue());
		} else {
			var evaluator = new TermEvaluator<>(term);
			new ExpressionEvaluation(body, evaluator, variable);
		}
	}

	private void translateCheckLiteral(CheckLiteral checkLiteral, PBody body) {
		var evaluator = new CheckEvaluator(checkLiteral.getTerm());
		new ExpressionEvaluation(body, evaluator, null);
	}

	private void translateCountLiteral(CountLiteral countLiteral, PBody body) {
		var wrappedCall = wrapperFactory.maybeWrapConstraint(countLiteral);
		var substitution = translateSubstitution(wrappedCall.remappedArguments(), body);
		var resultVariable = body.getOrCreateVariableByName(countLiteral.getResultVariable().getUniqueName());
		new PatternMatchCounter(body, substitution, wrappedCall.pattern(), resultVariable);
	}

	private <R, T> void translateAggregationLiteral(AggregationLiteral<R, T> aggregationLiteral, PBody body) {
		var aggregator = aggregationLiteral.getAggregator();
		IMultisetAggregationOperator<T, ?, R> aggregationOperator;
		if (aggregator instanceof StatelessAggregator<R, T> statelessAggregator) {
			aggregationOperator = new StatelessMultisetAggregator<>(statelessAggregator);
		} else if (aggregator instanceof StatefulAggregator<R, T> statefulAggregator) {
			aggregationOperator = new StatefulMultisetAggregator<>(statefulAggregator);
		} else {
			throw new IllegalArgumentException("Unknown aggregator: " + aggregator);
		}
		var wrappedCall = wrapperFactory.maybeWrapConstraint(aggregationLiteral);
		var substitution = translateSubstitution(wrappedCall.remappedArguments(), body);
		var inputVariable = body.getOrCreateVariableByName(aggregationLiteral.getInputVariable().getUniqueName());
		var aggregatedColumn = substitution.invertIndex().get(inputVariable);
		if (aggregatedColumn == null) {
			throw new IllegalStateException("Input variable %s not found in substitution %s".formatted(inputVariable,
					substitution));
		}
		var boundAggregator = new BoundAggregator(aggregationOperator, aggregator.getInputType(),
				aggregator.getResultType());
		var resultVariable = body.getOrCreateVariableByName(aggregationLiteral.getResultVariable().getUniqueName());
		new AggregatorConstraint(boundAggregator, body, substitution, wrappedCall.pattern(), resultVariable,
				aggregatedColumn);
	}
}
