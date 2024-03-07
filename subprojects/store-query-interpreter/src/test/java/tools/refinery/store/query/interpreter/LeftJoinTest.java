/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.int_.IntTerms;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertNullableResults;
import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertResults;

class LeftJoinTest {
	private static final Symbol<Boolean> person = Symbol.of("Person", 1);
	private static final Symbol<Integer> age = Symbol.of("age", 1, Integer.class);
	private static final AnySymbolView personView = new KeyOnlyView<>(person);
	private static final FunctionView<Integer> ageView = new FunctionView<>(age);

	@Test
	void unarySymbolTest() {
		var query = Query.of("Query", Integer.class, (builder, p1, output) -> builder
				.clause(
						personView.call(p1),
						output.assign(ageView.leftJoin(18, p1))
				));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		ageInterpretation.put(Tuple.of(2), 24);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.of(18),
				Tuple.of(1), Optional.of(18),
				Tuple.of(2), Optional.of(24),
				Tuple.of(3), Optional.empty()
		), queryResultSet);

		personInterpretation.put(Tuple.of(0), false);

		ageInterpretation.put(Tuple.of(1), 20);
		ageInterpretation.put(Tuple.of(2), null);

		queryEngine.flushChanges();
		assertNullableResults(Map.of(
				Tuple.of(0), Optional.empty(),
				Tuple.of(1), Optional.of(20),
				Tuple.of(2), Optional.of(18),
				Tuple.of(3), Optional.empty()
		), queryResultSet);
	}

	@Test
	void unarySymbolWithAssignmentTest() {
		// Tests an edge case where the outer joined variable is already bound in the query plan.
		var query = Query.of("Query", (builder, p1) -> builder
				.clause(Integer.class, v1 -> List.of(
						personView.call(p1),
						v1.assign(IntTerms.constant(18)),
						v1.assign(ageView.leftJoin(18, p1))
				)));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.queries(query))
				.build();

		var model = store.createEmptyModel();
		var personInterpretation = model.getInterpretation(person);
		var ageInterpretation = model.getInterpretation(age);
		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var queryResultSet = queryEngine.getResultSet(query);

		personInterpretation.put(Tuple.of(0), true);
		personInterpretation.put(Tuple.of(1), true);
		personInterpretation.put(Tuple.of(2), true);

		ageInterpretation.put(Tuple.of(2), 24);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), true,
				Tuple.of(1), true,
				Tuple.of(2), false,
				Tuple.of(3), false
		), queryResultSet);

		personInterpretation.put(Tuple.of(0), false);

		ageInterpretation.put(Tuple.of(1), 20);
		ageInterpretation.put(Tuple.of(2), null);

		queryEngine.flushChanges();
		assertResults(Map.of(
				Tuple.of(0), false,
				Tuple.of(1), false,
				Tuple.of(2), true,
				Tuple.of(3), false
		), queryResultSet);
	}
}
