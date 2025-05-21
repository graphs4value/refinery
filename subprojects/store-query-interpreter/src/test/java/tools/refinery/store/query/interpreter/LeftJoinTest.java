/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.literal.Literals;
import tools.refinery.logic.term.int_.IntTerms;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertNullableResults;
import static tools.refinery.store.query.interpreter.tests.QueryAssertions.assertResults;

class LeftJoinTest {
	private static final Symbol<Boolean> person = Symbol.of("Person", 1);
	private static final Symbol<Integer> age = Symbol.of("age", 1, Integer.class);
	private static final Symbol<Double> height = Symbol.of("height", 1, Double.class);
	private static final AnySymbolView personView = new KeyOnlyView<>(person);
	private static final FunctionView<Integer> ageView = new FunctionView<>(age);
	private static final FunctionView<Double> heightView = new FunctionView<>(height);

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

		try (var model = store.createEmptyModel()) {
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

		try (var model = store.createEmptyModel()) {
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

	@Test
	void updateDefaultTest() {
		var query = Query.of("Query", Integer.class, (builder, p1, output) -> builder
                .clause(
                        personView.call(p1),
                        output.assign(ageView.leftJoin(0, p1))
                ));

        var store = ModelStore.builder()
                .symbols(person, age)
                .with(QueryInterpreterAdapter.builder()
                        .queries(query))
                .build();

		try (var model = store.createEmptyModel()) {
			var personInterpretation = model.getInterpretation(person);
			var ageInterpretation = model.getInterpretation(age);
			var queryEngine = model.getAdapter(ModelQueryAdapter.class);
			var queryResultSet = queryEngine.getResultSet(query);

			personInterpretation.put(Tuple.of(0), true);

			queryEngine.flushChanges();
			assertResults(Map.of(Tuple.of(0), 0), queryResultSet);

			ageInterpretation.put(Tuple.of(0), 0);

			queryEngine.flushChanges();
			assertResults(Map.of(Tuple.of(0), 0), queryResultSet);

			ageInterpretation.put(Tuple.of(0), null);

			queryEngine.flushChanges();
			assertResults(Map.of(Tuple.of(0), 0), queryResultSet);
		}
	}

	@Test
	void multipleArgumentsTest() {
		var helper = Query.of("Helper", Integer.class, (builder, p1, p2, output) -> builder
				.clause(
						personView.call(p1),
						personView.call(p2),
						output.assign(IntTerms.mul(ageView.leftJoin(1, p1), ageView.leftJoin(1, p2))),
						Literals.check(IntTerms.greaterEq(output, IntTerms.constant(100)))
				));
		var query = Query.of("Query", Integer.class, (builder, p1, p2, output) -> builder
				.clause(
						personView.call(p1),
						personView.call(p2),
						output.assign(helper.leftJoin(0, p1, p2))
				));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.queries(query))
				.build();

		try (var model = store.createEmptyModel()) {
			var personInterpretation = model.getInterpretation(person);
			var ageInterpretation = model.getInterpretation(age);
			var queryEngine = model.getAdapter(ModelQueryAdapter.class);
			var queryResultSet = queryEngine.getResultSet(query);

			personInterpretation.put(Tuple.of(0), true);
			personInterpretation.put(Tuple.of(1), true);

			queryEngine.flushChanges();
			assertResults(Map.of(
					Tuple.of(0, 0), 0,
					Tuple.of(0, 1), 0,
					Tuple.of(1, 0), 0,
					Tuple.of(1, 1), 0
			), queryResultSet);

			ageInterpretation.put(Tuple.of(0), 100);

			queryEngine.flushChanges();
			assertResults(Map.of(
					Tuple.of(0, 0), 10000,
					Tuple.of(0, 1), 100,
					Tuple.of(1, 0), 100,
					Tuple.of(1, 1), 0
			), queryResultSet);

			ageInterpretation.put(Tuple.of(0), null);

			queryEngine.flushChanges();
			assertResults(Map.of(
					Tuple.of(0, 0), 0,
					Tuple.of(0, 1), 0,
					Tuple.of(1, 0), 0,
					Tuple.of(1, 1), 0
			), queryResultSet);
		}
	}

	@Test
	void multipleArgumentsDiagonalTest() {
		var helper = Query.of("Helper", Integer.class, (builder, p1, p2, output) -> builder
				.clause(
						personView.call(p1),
						personView.call(p2),
						output.assign(IntTerms.mul(ageView.leftJoin(1, p1), ageView.leftJoin(1, p2))),
						Literals.check(IntTerms.greaterEq(output, IntTerms.constant(100)))
				));
		var query = Query.of("Query", Integer.class, (builder, p1, output) -> builder
				.clause(
						personView.call(p1),
						output.assign(helper.leftJoin(0, p1, p1))
				));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.queries(query))
				.build();

		try (var model = store.createEmptyModel()) {
			var personInterpretation = model.getInterpretation(person);
			var ageInterpretation = model.getInterpretation(age);
			var queryEngine = model.getAdapter(ModelQueryAdapter.class);
			var queryResultSet = queryEngine.getResultSet(query);

			personInterpretation.put(Tuple.of(0), true);
			personInterpretation.put(Tuple.of(1), true);

			queryEngine.flushChanges();
			assertResults(Map.of(
					Tuple.of(0), 0,
					Tuple.of(1), 0
			), queryResultSet);

			ageInterpretation.put(Tuple.of(0), 10);

			queryEngine.flushChanges();
			assertResults(Map.of(
					Tuple.of(0), 100,
					Tuple.of(1), 0
			), queryResultSet);

			ageInterpretation.put(Tuple.of(0), null);

			queryEngine.flushChanges();
			assertResults(Map.of(
					Tuple.of(0), 0,
					Tuple.of(1), 0
			), queryResultSet);
		}
	}

	@Test
	void nonLeftInheritanceTest() {
		var helper = Dnf.of("Helper", builder -> {
			var output = builder.parameter("output", Integer.class);
			var p1 = builder.parameter("p1");
			builder.clause(
					ageView.call(p1, output)
			);
		});
		var query = Query.of("Query", Integer.class, (builder, p1, output) -> builder
				.clause(Integer.class, v1 -> List.of(
						personView.call(p1),
						output.assign(helper.leftJoinBy(v1, 18, v1, p1))
				)));

		var store = ModelStore.builder()
				.symbols(person, age)
				.with(QueryInterpreterAdapter.builder()
						.queries(query))
				.build();

		try (var model = store.createEmptyModel()) {
			var personInterpretation = model.getInterpretation(person);
			var ageInterpretation = model.getInterpretation(age);
			var queryEngine = model.getAdapter(ModelQueryAdapter.class);
			var queryResultSet = queryEngine.getResultSet(query);

			personInterpretation.put(Tuple.of(0), true);
			personInterpretation.put(Tuple.of(1), true);

			queryEngine.flushChanges();
			assertResults(Map.of(
					Tuple.of(0), 18,
					Tuple.of(1), 18
			), queryResultSet);

			ageInterpretation.put(Tuple.of(0), 10);

			queryEngine.flushChanges();
			assertResults(Map.of(
					Tuple.of(0), 10,
					Tuple.of(1), 18
			), queryResultSet);

			ageInterpretation.put(Tuple.of(0), null);

			queryEngine.flushChanges();
			assertResults(Map.of(
					Tuple.of(0), 18,
					Tuple.of(1), 18
			), queryResultSet);
		}
	}

	@ParameterizedTest
	@MethodSource
	void nanTest(Double defaultValue, Double valueToPut) {
		var query = Query.of("Query", Double.class, (builder, p1, output) -> builder
				.clause(
						personView.call(p1),
						output.assign(heightView.leftJoin(defaultValue, p1))
				));

		var store = ModelStore.builder()
				.symbols(person, height)
				.with(QueryInterpreterAdapter.builder()
						.queries(query))
				.build();

		try (var model = store.createEmptyModel()) {
			var personInterpretation = model.getInterpretation(person);
			var heightInterpretation = model.getInterpretation(height);
			var queryEngine = model.getAdapter(ModelQueryAdapter.class);
			var queryResultSet = queryEngine.getResultSet(query);

			personInterpretation.put(Tuple.of(0), true);

			queryEngine.flushChanges();
			assertResults(Map.of(Tuple.of(0), defaultValue), queryResultSet);

			heightInterpretation.put(Tuple.of(0), valueToPut);

			queryEngine.flushChanges();
			assertResults(Map.of(Tuple.of(0), valueToPut), queryResultSet);

			heightInterpretation.put(Tuple.of(0), null);

			queryEngine.flushChanges();
			assertResults(Map.of(Tuple.of(0), defaultValue), queryResultSet);
		}
	}

	static Stream<Arguments> nanTest() {
		return Stream.of(
				Arguments.of(Double.NaN, 180.0),
				Arguments.of(Double.NaN, Double.NaN),
				Arguments.of(0.0, Double.NaN)
		);
	}
}
