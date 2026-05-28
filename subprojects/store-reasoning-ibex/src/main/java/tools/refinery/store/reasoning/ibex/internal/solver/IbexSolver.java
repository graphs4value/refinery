/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.ibex.internal.solver;

import ibex.Ibex;
import tools.refinery.logic.AbstractValue;
import tools.refinery.store.dse.propagation.PropagationRejectedResult;
import tools.refinery.store.dse.propagation.PropagationResult;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.query.resultset.ResultSetListener;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ibex.internal.PreparedIbexRule;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.List;

public class IbexSolver implements ResultSetListener<Boolean> {
	private static final String REJECTION_EMPTY = "IBEX contracted a domain to empty";

	private final Object reason;
	private final PreparedIbexRule rule;
	private final Model model;
	private final ResultSet<Boolean> resultSet;
	private List<PartialFunctionMonitor<?, ?>> monitors;
	private boolean started = false;
	private boolean changed = true;

	public IbexSolver(Object reason, PreparedIbexRule rule, Model model) {
		this.reason = reason;
		this.rule = rule;
		this.model = model;

		var queryAdapter = model.getAdapter(ModelQueryAdapter.class);
		resultSet = queryAdapter.getResultSet(rule.partialPrecondition());
	}

	public boolean isChanged() {
		return changed;
	}

	public void markChanged() {
		changed = true;
	}

	private void startIfNeeded() {
		if (started) {
			return;
		}
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		var influences = rule.influences();
		monitors = new ArrayList<>(influences.size());
		for (var influence : influences) {
			monitors.add(createMonitor((PartialFunction<?, ?>) influence.partialFunction(), reasoningAdapter));
		}
		var cursor = resultSet.getAll();
		while (cursor.move()) {
			changeRefs(cursor.getKey(), true);
		}
		resultSet.addListener(this);
		started = true;
	}

	private <A extends AbstractValue<A, C>, C> PartialFunctionMonitor<A, C> createMonitor(
			PartialFunction<A, C> pf, ReasoningAdapter ra) {
		return new PartialFunctionMonitor<>(this, pf, ra);
	}

	@Override
	public void put(Tuple key, Boolean fromValue, Boolean toValue) {
		boolean wasActive = Boolean.TRUE.equals(fromValue);
		boolean isActive = Boolean.TRUE.equals(toValue);
		if (wasActive != isActive) {
			changeRefs(key, isActive);
		}
	}

	private void changeRefs(Tuple matchKey, boolean add) {
		markChanged();
		var influences = rule.influences();
		for (int i = 0; i < influences.size(); i++) {
			var nodeTuple = extractNodeTuple(matchKey, influences.get(i).parameterIndices());
			if (add) {
				monitors.get(i).addRef(nodeTuple);
			} else {
				monitors.get(i).removeRef(nodeTuple);
			}
		}
	}

	public PropagationResult propagate() {
		startIfNeeded();
		if (!changed) {
			return PropagationResult.UNCHANGED;
		}
		changed = false;

		var overall = PropagationResult.UNCHANGED;
		var cursor = resultSet.getAll();
		while (cursor.move()) {
			overall = overall.andThen(propagateMatch(cursor.getKey()));
			if (overall.isRejected()) {
				return overall;
			}
		}
		return overall;
	}

	private PropagationResult propagateMatch(Tuple matchKey) {
		var influences = rule.influences();
		int numVars = influences.size();
		var domains = new double[numVars * 2];

		for (int i = 0; i < numVars; i++) {
			var nodeTuple = extractNodeTuple(matchKey, influences.get(i).parameterIndices());
			if (!monitors.get(i).fillDomain(nodeTuple, domains, i)) {
				return new PropagationRejectedResult(reason, REJECTION_EMPTY);
			}
		}

		int status = rule.ibex().contract(0, domains);

		return switch (status) {
			case Ibex.FAIL -> new PropagationRejectedResult(reason, REJECTION_EMPTY);
			case Ibex.ENTAILED, Ibex.NOTHING -> PropagationResult.UNCHANGED;
			case Ibex.CONTRACT -> applyContracted(matchKey, domains);
			default -> PropagationResult.UNCHANGED;
		};
	}

	private PropagationResult applyContracted(Tuple matchKey, double[] domains) {
		var influences = rule.influences();
		var overall = PropagationResult.UNCHANGED;
		for (int i = 0; i < influences.size(); i++) {
			var nodeTuple = extractNodeTuple(matchKey, influences.get(i).parameterIndices());
			overall = overall.andThen(monitors.get(i).applyDomain(nodeTuple, domains, i, reason));
			if (overall.isRejected()) {
				return overall;
			}
		}
		return overall;
	}

	private static Tuple extractNodeTuple(Tuple matchKey, Tuple paramIndices) {
		int arity = paramIndices.getSize();
		int[] nodes = new int[arity];
		for (int i = 0; i < arity; i++) {
			nodes[i] = matchKey.get(paramIndices.get(i));
		}
		return Tuple.of(nodes);
	}
}
