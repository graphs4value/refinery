/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.solver;

import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.query.resultset.ResultSetListener;
import tools.refinery.store.reasoning.smt.internal.PreparedSmtRule;
import tools.refinery.store.tuple.Tuple;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RuleMonitor implements ResultSetListener<Boolean> {
	private final RuleBasedSolver solver;
	private final PreparedSmtRule rule;
	private final ResultSet<Boolean> resultSet;
	private final Set<Tuple> dirty = new LinkedHashSet<>();

	private record Source(RuleMonitor monitor, Tuple tuple) {
	}

	public RuleMonitor(RuleBasedSolver solver, PreparedSmtRule rule) {
		this.solver = solver;
		this.rule = rule;
		this.resultSet = solver.getContext().getResultSet(rule.getQuery(solver.getConcreteness()));
	}

	public void start() {
		var cursor = resultSet.getAll();
		while (cursor.move()) {
			changeRefs(cursor.getKey(), true);
		}
		resultSet.addListener(this);
	}

	public void synchronizeAssertions() {
		for (var tuple : dirty) {
			var source = new Source(this, tuple);
			if (Boolean.TRUE.equals(resultSet.get(tuple))) {
				solver.putAssertions(source, null, List.of(solver.getContext().getExpr(rule, tuple)));
			} else {
				solver.removeAssertions(source);
			}
		}
		dirty.clear();
	}

	@Override
	public void put(Tuple key, Boolean fromValue, Boolean toValue) {
		if (!Objects.equals(fromValue, toValue)) {
			changeRefs(key, toValue);
		}
	}

	private void changeRefs(Tuple key, boolean add) {
		solver.markChanged();
		dirty.add(key);
		for (var influence : rule.influences()) {
			var indexTuple = influence.parameterIndices();
			int arity = indexTuple.getSize();
			var nodeArray = new int[arity];
			for (int i = 0; i < arity; i++) {
				nodeArray[i] = key.get(indexTuple.get(i));
			}
			var nodeTuple = Tuple.of(nodeArray);
			solver.changeRef(influence.partialFunction(), nodeTuple, add);
		}
	}
}
