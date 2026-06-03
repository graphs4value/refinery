/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.internal.solver;

import com.microsoft.z3.Solver;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.query.resultset.ResultSetListener;
import tools.refinery.store.reasoning.smt.internal.PreparedSmtRule;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

public class RuleMonitor implements ResultSetListener<Boolean> {
	private final RuleBasedSolver solver;
	private final PreparedSmtRule rule;
	private final ResultSet<Boolean> resultSet;

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

	// Z3 assertions create unchecked generic arrays.
	@SuppressWarnings("unchecked")
	public void addAssertions(Solver z3solver) {
		var context = solver.getContext();
		var cursor = resultSet.getAll();
		while (cursor.move()) {
			var key = cursor.getKey();
			z3solver.add(context.getExpr(rule, key));
		}
	}

	@Override
	public void put(Tuple key, Boolean fromValue, Boolean toValue) {
		if (!Objects.equals(fromValue, toValue)) {
			changeRefs(key, toValue);
		}
	}

	private void changeRefs(Tuple key, boolean add) {
		solver.markChanged();
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
