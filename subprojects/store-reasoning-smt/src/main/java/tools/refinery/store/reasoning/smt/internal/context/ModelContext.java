/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.context;

import com.microsoft.z3.*;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.realinterval.RealIntervalDomain;
import tools.refinery.logic.term.string.StringDomain;
import tools.refinery.logic.term.truthvalue.TruthValueDomain;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.AnyPartialFunction;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.smt.internal.PreparedSmtRule;
import tools.refinery.store.reasoning.smt.internal.solver.RuleBasedSolver;
import tools.refinery.store.tuple.Tuple;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class ModelContext implements AutoCloseable {
	public static final int SOLVER_TIMEOUT = 10000;

	private final Model model;
	private final ModelQueryAdapter queryEngine;
	private ReasoningAdapter reasoningAdapter;
	private final List<PreparedSmtRule> rules;
	private final Context context;
	private final Sort boolSort;
	private final Sort intSort;
	private final Sort realSort;
	private final Sort stringSort;
	private final TermToExpr termToExpr;
	private final Map<AnyPartialFunction, Map<Tuple, FuncDecl<?>>> variableCache = new HashMap<>();
	private final Map<PreparedSmtRule, Map<Tuple, Expr<?>>> exprCache = new HashMap<>();
	private final InterruptibleWrapper interruptibleWrapper;

	public ModelContext(Model model, Collection<PreparedSmtRule> rules) {
		this.model = model;
		queryEngine = model.getAdapter(ModelQueryAdapter.class);
		this.rules = List.copyOf(rules);
		context = new Context();
		boolSort = context.getBoolSort();
		intSort = context.getIntSort();
		realSort = context.getRealSort();
		stringSort = context.getStringSort();
		termToExpr = new TermToExpr(this);
		interruptibleWrapper = new InterruptibleWrapper(model.getCancellationToken(), context);
	}

	public Context getZ3Context() {
		return context;
	}

	public <T> ResultSet<T> getResultSet(Query<T> query) {
		return queryEngine.getResultSet(query);
	}

	public <A extends AbstractValue<A, C>, C> PartialInterpretation<A, C> getPartialInterpretation(
			Concreteness concreteness, PartialSymbol<A, C> partialSymbol) {
		return getReasoningAdapter().getPartialInterpretation(concreteness, partialSymbol);
	}

	public <A extends AbstractValue<A, C>, C> PartialInterpretationRefiner<A, C> getRefiner(
			PartialSymbol<A, C> partialSymbol) {
		return getReasoningAdapter().getRefiner(partialSymbol);
	}

	private ReasoningAdapter getReasoningAdapter() {
		if (reasoningAdapter == null) {
			reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		}
		return reasoningAdapter;
	}

	public RuleBasedSolver createSolver(Concreteness concreteness) {
		var solver = context.mkSolver();
		var params = context.mkParams();
		params.add("model", concreteness == Concreteness.CANDIDATE);
		params.add("timeout", SOLVER_TIMEOUT);
		solver.setParameters(params);
		return new RuleBasedSolver(this, concreteness, rules, solver);
	}

	public FuncDecl<?> getVariable(AnyPartialFunction partialFunction, Tuple input) {
		var cache = variableCache.computeIfAbsent(partialFunction, _ -> new HashMap<>());
		var sort = getSort(partialFunction);
		return cache.computeIfAbsent(input, key -> context.mkConstDecl(partialFunction.name() + key, sort));
	}

	private Sort getSort(AnyPartialFunction partialFunction) {
		var abstractDomain = partialFunction.abstractDomain();
		if (TruthValueDomain.INSTANCE.equals(abstractDomain)) {
			return boolSort;
		}
		if (IntIntervalDomain.INSTANCE.equals(abstractDomain)) {
			return intSort;
		}
		if (RealIntervalDomain.INSTANCE.equals(abstractDomain)) {
			return realSort;
		}
		if (StringDomain.INSTANCE.equals(abstractDomain)) {
			return stringSort;
		}
		throw new IllegalArgumentException("Unknown abstract domain %s for partial function %s"
				.formatted(abstractDomain, partialFunction));
	}

	public Expr<BoolSort> getExpr(PreparedSmtRule rule, Tuple input) {
		var cache = exprCache.computeIfAbsent(rule, _ -> new HashMap<>());
		// We know that this was translated from `Term<TruthValue>`.
		@SuppressWarnings("unchecked")
		var result = (Expr<BoolSort>) cache.computeIfAbsent(input, parameterTuple ->
				termToExpr.toExpr(rule.assertedTerm(), parameterTuple, rule.parameterMap()));
		return result;
	}

	public <T> T callWithInterrupt(Callable<T> callable) {
		return interruptibleWrapper.call(callable);
	}

	@Override
	public void close() {
		interruptibleWrapper.shutdown();
	}
}
