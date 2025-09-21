/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.int_.IntTerms;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.internal.pquery.OptimizationBarrier;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;
import java.util.Map;

import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertResults;

class OuterIndexerTest {
	private final Symbol<Boolean> person = Symbol.of("Person", 1);
	private final Symbol<Boolean> friend = Symbol.of("friend", 2);
	private final Symbol<Integer> age = Symbol.of("age", 1, Integer.class);
	private final Symbol<Integer> friendAge = Symbol.of("friendAge", 2, Integer.class);
	private final AnySymbolView personView = new KeyOnlyView<>(person);
	private final AnySymbolView friendView = new KeyOnlyView<>(friend);
	private final FunctionView<Integer> ageView = new FunctionView<>(age);
	private final FunctionView<Integer> friendAgeView = new FunctionView<>(friendAge);

	@Test
	void leftJoinTest() {
		var query = Query.of("Query", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, (v1, v2, v3) -> List.of(
						personView.call(p1),
						v1.assign(new OptimizationBarrier<>(ageView.leftJoin(0, p1))),
						v2.assign(new OptimizationBarrier<>(ageView.leftJoin(0, p1))),
						v3.assign(new OptimizationBarrier<>(ageView.leftJoin(0, p1))),
						output.assign(IntTerms.add(IntTerms.add(v1, v2), v3))
				)));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.query(query))
				.build();

		try (var model = store.createEmptyModel()) {
			var personInterpretation = model.getInterpretation(person);
			var ageInterpretation = model.getInterpretation(age);
			var queryEngine = model.getAdapter(QueryInterpreterAdapter.class);
			var resultSet = queryEngine.getResultSet(query);

			personInterpretation.put(Tuple.of(0), true);
			queryEngine.flushChanges();

			assertResults(Map.of(
					Tuple.of(0), 0
			), resultSet);

			ageInterpretation.put(Tuple.of(0), 24);
			// This triggers {@code Duplicate deletion of tuple T([0];24;0;)} with the buggy {@code OuterIndexer}.
			queryEngine.flushChanges();

			assertResults(Map.of(
					Tuple.of(0), 72
			), resultSet);
		}
	}

	@Test
	void countTest() {
		var query = Query.of("Query", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, (v1, v2, v3) -> List.of(
						personView.call(p1),
						v1.assign(new OptimizationBarrier<>(friendView.count(p1, Variable.of()))),
						v2.assign(new OptimizationBarrier<>(friendView.count(p1, Variable.of()))),
						v3.assign(new OptimizationBarrier<>(friendView.count(p1, Variable.of()))),
						output.assign(IntTerms.add(IntTerms.add(v1, v2), v3))
				)));

		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder()
						.query(query))
				.build();

		try (var model = store.createEmptyModel()) {
			var personInterpretation = model.getInterpretation(person);
			var friendInterpretation = model.getInterpretation(friend);
			var queryEngine = model.getAdapter(QueryInterpreterAdapter.class);
			var resultSet = queryEngine.getResultSet(query);

			personInterpretation.put(Tuple.of(0), true);
			personInterpretation.put(Tuple.of(1), true);
			queryEngine.flushChanges();

			assertResults(Map.of(
					Tuple.of(0), 0,
					Tuple.of(1), 0
			), resultSet);

			friendInterpretation.put(Tuple.of(0, 1), true);
			friendInterpretation.put(Tuple.of(1, 0), true);
			// This triggers {@code Duplicate deletion of tuple T([1];1;0;)} with the buggy {@code OuterIndexer}.
			queryEngine.flushChanges();

			assertResults(Map.of(
					Tuple.of(0), 3,
					Tuple.of(1), 3
			), resultSet);
		}
	}

	@Test
	void aggregateTest() {
		var helper = Query.of("Helper", Integer.class, (builder, p1, p2, output) -> builder
				.clause(
						friendView.call(p1, p2),
						output.assign(ageView.leftJoin(0, p2))
				));

		var query = Query.of("Query", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, (v1, v2, v3) -> List.of(
						personView.call(p1),
						v1.assign(new OptimizationBarrier<>(helper.aggregate(IntTerms.INT_SUM, p1, Variable.of()))),
						v2.assign(new OptimizationBarrier<>(helper.aggregate(IntTerms.INT_SUM, p1, Variable.of()))),
						v3.assign(new OptimizationBarrier<>(helper.aggregate(IntTerms.INT_SUM, p1, Variable.of()))),
						output.assign(IntTerms.add(IntTerms.add(v1, v2), v3))
				)));

		var store = ModelStore.builder()
				.symbols(person, friend, age)
				.with(QueryInterpreterAdapter.builder()
						.query(query))
				.build();

		try (var model = store.createEmptyModel()) {
			var personInterpretation = model.getInterpretation(person);
			var ageInterpretation = model.getInterpretation(age);
			var friendInterpretation = model.getInterpretation(friend);
			var queryEngine = model.getAdapter(QueryInterpreterAdapter.class);
			var resultSet = queryEngine.getResultSet(query);

			personInterpretation.put(Tuple.of(0), true);
			personInterpretation.put(Tuple.of(1), true);
			ageInterpretation.put(Tuple.of(0), 24);
			ageInterpretation.put(Tuple.of(1), 25);
			queryEngine.flushChanges();

			assertResults(Map.of(
					Tuple.of(0), 0,
					Tuple.of(1), 0
			), resultSet);

			friendInterpretation.put(Tuple.of(0, 1), true);
			friendInterpretation.put(Tuple.of(1, 0), true);
			// This does not trigger a bug even in the unmodified implementation.
			queryEngine.flushChanges();

			assertResults(Map.of(
					Tuple.of(0), 75,
					Tuple.of(1), 72
			), resultSet);
		}
	}

	@Test
	void aggregateSymbolTest() {
		var query = Query.of("Query", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, Integer.class, Integer.class, (v1, v2, v3) -> List.of(
						personView.call(p1),
						v1.assign(new OptimizationBarrier<>(friendAgeView.aggregate(IntTerms.INT_SUM, p1,
								Variable.of()))),
						v2.assign(new OptimizationBarrier<>(friendAgeView.aggregate(IntTerms.INT_SUM, p1,
								Variable.of()))),
						v3.assign(new OptimizationBarrier<>(friendAgeView.aggregate(IntTerms.INT_SUM, p1,
								Variable.of()))),
						output.assign(IntTerms.add(IntTerms.add(v1, v2), v3))
				)));

		var store = ModelStore.builder()
				.symbols(person, friendAge)
				.with(QueryInterpreterAdapter.builder()
						.query(query))
				.build();

		try (var model = store.createEmptyModel()) {
			var personInterpretation = model.getInterpretation(person);
			var friendAgeInterpretation = model.getInterpretation(friendAge);
			var queryEngine = model.getAdapter(QueryInterpreterAdapter.class);
			var resultSet = queryEngine.getResultSet(query);

			personInterpretation.put(Tuple.of(0), true);
			personInterpretation.put(Tuple.of(1), true);
			queryEngine.flushChanges();

			assertResults(Map.of(
					Tuple.of(0), 0,
					Tuple.of(1), 0
			), resultSet);

			friendAgeInterpretation.put(Tuple.of(0, 1), 25);
			friendAgeInterpretation.put(Tuple.of(1, 0), 24);
			// This does not trigger a bug even in the unmodified implementation.
			queryEngine.flushChanges();

			assertResults(Map.of(
					Tuple.of(0), 75,
					Tuple.of(1), 72
			), resultSet);
		}
	}
}
