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
import org.eclipse.viatra.query.runtime.matchers.psystem.queries.PQuery;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuple;
import org.eclipse.viatra.query.runtime.matchers.tuple.Tuples;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.DnfClause;
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

		Map<Variable, PParameter> parameters = new HashMap<>();
		for (Variable variable : dnfQuery.getParameters()) {
			parameters.put(variable, new PParameter(variable.getUniqueName()));
		}

		List<PParameter> parameterList = new ArrayList<>();
		for (var param : dnfQuery.getParameters()) {
			parameterList.add(parameters.get(param));
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
				List<ExportedParameter> symbolicParameters = new ArrayList<>();
				for (var param : dnfQuery.getParameters()) {
					PVariable pVar = body.getOrCreateVariableByName(param.getUniqueName());
					symbolicParameters.add(new ExportedParameter(body, pVar, parameters.get(param)));
				}
				body.setSymbolicParameters(symbolicParameters);
				pQuery.addBody(body);
				for (Literal literal : clause.literals()) {
					translateLiteral(literal, clause, body);
				}
			}
		}

		return pQuery;
	}

	private void translateLiteral(Literal literal, DnfClause clause, PBody body) {
		if (literal instanceof EquivalenceLiteral equivalenceLiteral) {
			translateEquivalenceLiteral(equivalenceLiteral, body);
		} else if (literal instanceof CallLiteral callLiteral) {
			translateCallLiteral(callLiteral, clause, body);
		} else if (literal instanceof ConstantLiteral constantLiteral) {
			translateConstantLiteral(constantLiteral, body);
		} else if (literal instanceof AssignLiteral<?> assignLiteral) {
			translateAssignLiteral(assignLiteral, body);
		} else if (literal instanceof AssumeLiteral assumeLiteral) {
			translateAssumeLiteral(assumeLiteral, body);
		} else if (literal instanceof CountLiteral countLiteral) {
			translateCountLiteral(countLiteral, clause, body);
		} else if (literal instanceof AggregationLiteral<?, ?> aggregationLiteral) {
			translateAggregationLiteral(aggregationLiteral, clause, body);
		} else {
			throw new IllegalArgumentException("Unknown literal: " + literal.toString());
		}
	}

	private void translateEquivalenceLiteral(EquivalenceLiteral equivalenceLiteral, PBody body) {
		PVariable varSource = body.getOrCreateVariableByName(equivalenceLiteral.left().getUniqueName());
		PVariable varTarget = body.getOrCreateVariableByName(equivalenceLiteral.right().getUniqueName());
		if (equivalenceLiteral.positive()) {
			new Equality(body, varSource, varTarget);
		} else {
			new Inequality(body, varSource, varTarget);
		}
	}

	private void translateCallLiteral(CallLiteral callLiteral, DnfClause clause, PBody body) {
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
			var wrappedCall = wrapperFactory.maybeWrapConstraint(callLiteral, clause);
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
		var variable = body.getOrCreateVariableByName(constantLiteral.variable().getUniqueName());
		new ConstantValue(body, variable, constantLiteral.nodeId());
	}

	private <T> void translateAssignLiteral(AssignLiteral<T> assignLiteral, PBody body) {
		var variable = body.getOrCreateVariableByName(assignLiteral.variable().getUniqueName());
		var term = assignLiteral.term();
		if (term instanceof ConstantTerm<T> constantTerm) {
			new ConstantValue(body, variable, constantTerm.getValue());
		} else {
			var evaluator = new TermEvaluator<>(term);
			new ExpressionEvaluation(body, evaluator, variable);
		}
	}

	private void translateAssumeLiteral(AssumeLiteral assumeLiteral, PBody body) {
		var evaluator = new AssumptionEvaluator(assumeLiteral.term());
		new ExpressionEvaluation(body, evaluator, null);
	}

	private void translateCountLiteral(CountLiteral countLiteral, DnfClause clause, PBody body) {
		var wrappedCall = wrapperFactory.maybeWrapConstraint(countLiteral, clause);
		var substitution = translateSubstitution(wrappedCall.remappedArguments(), body);
		var resultVariable = body.getOrCreateVariableByName(countLiteral.getResultVariable().getUniqueName());
		new PatternMatchCounter(body, substitution, wrappedCall.pattern(), resultVariable);
	}

	private <R, T> void translateAggregationLiteral(AggregationLiteral<R, T> aggregationLiteral, DnfClause clause,
													PBody body) {
		var aggregator = aggregationLiteral.getAggregator();
		IMultisetAggregationOperator<T, ?, R> aggregationOperator;
		if (aggregator instanceof StatelessAggregator<R, T> statelessAggregator) {
			aggregationOperator = new StatelessMultisetAggregator<>(statelessAggregator);
		} else if (aggregator instanceof StatefulAggregator<R, T> statefulAggregator) {
			aggregationOperator = new StatefulMultisetAggregator<>(statefulAggregator);
		} else {
			throw new IllegalArgumentException("Unknown aggregator: " + aggregator);
		}
		var wrappedCall = wrapperFactory.maybeWrapConstraint(aggregationLiteral, clause);
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
