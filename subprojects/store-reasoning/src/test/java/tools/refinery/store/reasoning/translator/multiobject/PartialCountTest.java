/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.CountLowerBoundLiteral;
import tools.refinery.store.reasoning.literal.CountUpperBoundLiteral;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.representation.cardinality.UpperCardinalities;
import tools.refinery.store.representation.cardinality.UpperCardinality;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.store.query.literal.Literals.not;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;

class PartialCountTest {
	private static final PartialRelation person = new PartialRelation("Person", 1);
	private static final PartialRelation friend = new PartialRelation("friend", 2);

	@Test
	void lowerBoundZeroTest() {
		var query = Query.of("LowerBound", Integer.class, (builder, p1, p2, output) -> builder.clause(
				must(person.call(p1)),
				must(person.call(p2)),
				new CountLowerBoundLiteral(output, friend, List.of(p1, p2))
		));

		var modelSeed = ModelSeed.builder(2)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.put(Tuple.of(0), CardinalityIntervals.atLeast(3))
						.put(Tuple.of(1), CardinalityIntervals.atMost(7)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.seed(friend, builder -> builder
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(1, 0), TruthValue.UNKNOWN)
						.put(Tuple.of(1, 1), TruthValue.ERROR))
				.build();

		var resultSet = getResultSet(query, modelSeed);
		assertThat(resultSet.get(Tuple.of(0, 0)), is(0));
		assertThat(resultSet.get(Tuple.of(0, 1)), is(1));
		assertThat(resultSet.get(Tuple.of(1, 0)), is(0));
		assertThat(resultSet.get(Tuple.of(1, 1)), is(1));
	}

	@Test
	void upperBoundZeroTest() {
		var query = Query.of("UpperBound", UpperCardinality.class, (builder, p1, p2, output) -> builder.clause(
				must(person.call(p1)),
				must(person.call(p2)),
				new CountUpperBoundLiteral(output, friend, List.of(p1, p2))
		));

		var modelSeed = ModelSeed.builder(2)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.put(Tuple.of(0), CardinalityIntervals.atLeast(3))
						.put(Tuple.of(1), CardinalityIntervals.atMost(7)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.seed(friend, builder -> builder
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(1, 0), TruthValue.UNKNOWN)
						.put(Tuple.of(1, 1), TruthValue.ERROR))
				.build();

		var resultSet = getResultSet(query, modelSeed);
		assertThat(resultSet.get(Tuple.of(0, 0)), is(UpperCardinalities.ZERO));
		assertThat(resultSet.get(Tuple.of(0, 1)), is(UpperCardinalities.ONE));
		assertThat(resultSet.get(Tuple.of(1, 0)), is(UpperCardinalities.ONE));
		assertThat(resultSet.get(Tuple.of(1, 1)), is(UpperCardinalities.ZERO));
	}

	@Test
	void lowerBoundOneTest() {
		var query = Query.of("LowerBound", Integer.class, (builder, p1, output) -> builder.clause(
				must(person.call(p1)),
				new CountLowerBoundLiteral(output, friend, List.of(p1, Variable.of()))
		));

		var modelSeed = ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(1), CardinalityIntervals.atLeast(3))
						.put(Tuple.of(2), CardinalityIntervals.atMost(7)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.seed(friend, builder -> builder
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(0, 2), TruthValue.TRUE)
						.put(Tuple.of(0, 3), TruthValue.TRUE)
						.put(Tuple.of(1, 0), TruthValue.TRUE)
						.put(Tuple.of(1, 2), TruthValue.UNKNOWN)
						.put(Tuple.of(1, 3), TruthValue.UNKNOWN)
						.put(Tuple.of(2, 0), TruthValue.TRUE)
						.put(Tuple.of(2, 1), TruthValue.ERROR))
				.build();

		var resultSet = getResultSet(query, modelSeed);
		assertThat(resultSet.get(Tuple.of(0)), is(4));
		assertThat(resultSet.get(Tuple.of(1)), is(1));
		assertThat(resultSet.get(Tuple.of(2)), is(4));
		assertThat(resultSet.get(Tuple.of(3)), is(0));
	}

	@Test
	void upperBoundOneTest() {
		var query = Query.of("UpperBound", UpperCardinality.class, (builder, p1, output) -> builder.clause(
				must(person.call(p1)),
				new CountUpperBoundLiteral(output, friend, List.of(p1, Variable.of()))
		));

		var modelSeed = ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(1), CardinalityIntervals.atLeast(3))
						.put(Tuple.of(2), CardinalityIntervals.atMost(7)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.seed(friend, builder -> builder
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(0, 2), TruthValue.TRUE)
						.put(Tuple.of(0, 3), TruthValue.TRUE)
						.put(Tuple.of(1, 0), TruthValue.TRUE)
						.put(Tuple.of(1, 2), TruthValue.UNKNOWN)
						.put(Tuple.of(1, 3), TruthValue.UNKNOWN)
						.put(Tuple.of(2, 0), TruthValue.TRUE)
						.put(Tuple.of(2, 1), TruthValue.ERROR))
				.build();

		var resultSet = getResultSet(query, modelSeed);
		assertThat(resultSet.get(Tuple.of(0)), is(UpperCardinalities.UNBOUNDED));
		assertThat(resultSet.get(Tuple.of(1)), is(UpperCardinalities.atMost(9)));
		assertThat(resultSet.get(Tuple.of(2)), is(UpperCardinalities.ONE));
		assertThat(resultSet.get(Tuple.of(3)), is(UpperCardinalities.ZERO));
	}

	@Test
	void lowerBoundTwoTest() {
		var subQuery = Query.of("SubQuery", (builder, p1, p2, p3) -> builder.clause(
				friend.call(p1, p2),
				friend.call(p1, p3),
				friend.call(p2, p3)
		));
		var query = Query.of("LowerBound", Integer.class, (builder, p1, output) -> builder.clause(
				must(person.call(p1)),
				new CountLowerBoundLiteral(output, subQuery.getDnf(), List.of(p1, Variable.of(), Variable.of()))
		));

		var modelSeed = ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.between(5, 9))
						.put(Tuple.of(1), CardinalityIntervals.atLeast(3))
						.put(Tuple.of(2), CardinalityIntervals.atMost(7)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.seed(friend, builder -> builder
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(0, 2), TruthValue.TRUE)
						.put(Tuple.of(0, 3), TruthValue.TRUE)
						.put(Tuple.of(1, 0), TruthValue.TRUE)
						.put(Tuple.of(1, 2), TruthValue.TRUE)
						.put(Tuple.of(1, 3), TruthValue.TRUE)
						.put(Tuple.of(2, 0), TruthValue.TRUE)
						.put(Tuple.of(2, 1), TruthValue.ERROR))
				.build();

		var resultSet = getResultSet(query, modelSeed);
		assertThat(resultSet.get(Tuple.of(0)), is(3));
		assertThat(resultSet.get(Tuple.of(1)), is(5));
		assertThat(resultSet.get(Tuple.of(2)), is(30));
		assertThat(resultSet.get(Tuple.of(3)), is(0));
	}

	@Test
	void upperBoundTwoTest() {
		var subQuery = Query.of("SubQuery", (builder, p1, p2, p3) -> builder.clause(
				friend.call(p1, p2),
				friend.call(p1, p3),
				friend.call(p2, p3)
		));
		var query = Query.of("UpperBound", UpperCardinality.class, (builder, p1, output) -> builder.clause(
				must(person.call(p1)),
				new CountUpperBoundLiteral(output, subQuery.getDnf(), List.of(p1, Variable.of(), Variable.of()))
		));

		var modelSeed = ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.between(5, 9))
						.put(Tuple.of(1), CardinalityIntervals.atLeast(3))
						.put(Tuple.of(2), CardinalityIntervals.atMost(7)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.seed(friend, builder -> builder
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(0, 2), TruthValue.TRUE)
						.put(Tuple.of(0, 3), TruthValue.TRUE)
						.put(Tuple.of(1, 0), TruthValue.TRUE)
						.put(Tuple.of(1, 2), TruthValue.UNKNOWN)
						.put(Tuple.of(1, 3), TruthValue.UNKNOWN)
						.put(Tuple.of(2, 0), TruthValue.TRUE)
						.put(Tuple.of(2, 1), TruthValue.ERROR))
				.build();

		var resultSet = getResultSet(query, modelSeed);
		assertThat(resultSet.get(Tuple.of(0)), is(UpperCardinalities.UNBOUNDED));
		assertThat(resultSet.get(Tuple.of(1)), is(UpperCardinalities.atMost(135)));
		assertThat(resultSet.get(Tuple.of(2)), is(UpperCardinalities.ZERO));
		assertThat(resultSet.get(Tuple.of(3)), is(UpperCardinalities.ZERO));
	}

	@Test
	void lowerBoundDiagonalTest() {
		var subQuery = Query.of("SubQuery", (builder, p1, p2, p3) -> builder.clause(
				friend.call(p1, p2),
				friend.call(p1, p3),
				not(friend.call(p2, p3))
		));
		var query = Query.of("LowerBound", Integer.class, (builder, p1, output) -> builder.clause(v1 -> List.of(
				must(person.call(p1)),
				new CountLowerBoundLiteral(output, subQuery.getDnf(), List.of(p1, v1, v1))
		)));

		var modelSeed = ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.between(5, 9))
						.put(Tuple.of(1), CardinalityIntervals.atLeast(3))
						.put(Tuple.of(2), CardinalityIntervals.atMost(7)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.seed(friend, builder -> builder
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(0, 2), TruthValue.TRUE)
						.put(Tuple.of(0, 3), TruthValue.TRUE)
						.put(Tuple.of(1, 0), TruthValue.TRUE)
						.put(Tuple.of(1, 2), TruthValue.UNKNOWN)
						.put(Tuple.of(1, 3), TruthValue.UNKNOWN)
						.put(Tuple.of(2, 0), TruthValue.TRUE)
						.put(Tuple.of(2, 1), TruthValue.ERROR))
				.build();

		var resultSet = getResultSet(query, modelSeed);
		assertThat(resultSet.get(Tuple.of(0)), is(4));
		assertThat(resultSet.get(Tuple.of(1)), is(5));
		assertThat(resultSet.get(Tuple.of(2)), is(8));
		assertThat(resultSet.get(Tuple.of(3)), is(0));
	}

	@Test
	void upperBoundDiagonalTest() {
		var subQuery = Query.of("SubQuery", (builder, p1, p2, p3) -> builder.clause(
				friend.call(p1, p2),
				friend.call(p1, p3),
				not(friend.call(p2, p3))
		));
		var query = Query.of("UpperBound", UpperCardinality.class, (builder, p1, output) -> builder
				.clause(v1 -> List.of(
						must(person.call(p1)),
						new CountUpperBoundLiteral(output, subQuery.getDnf(), List.of(p1, v1, v1))
				)));

		var modelSeed = ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.between(5, 9))
						.put(Tuple.of(1), CardinalityIntervals.atLeast(3))
						.put(Tuple.of(2), CardinalityIntervals.atMost(7)))
				.seed(person, builder -> builder.reducedValue(TruthValue.TRUE))
				.seed(friend, builder -> builder
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(0, 2), TruthValue.TRUE)
						.put(Tuple.of(0, 3), TruthValue.TRUE)
						.put(Tuple.of(1, 0), TruthValue.TRUE)
						.put(Tuple.of(1, 2), TruthValue.UNKNOWN)
						.put(Tuple.of(1, 3), TruthValue.UNKNOWN)
						.put(Tuple.of(2, 0), TruthValue.TRUE)
						.put(Tuple.of(2, 1), TruthValue.ERROR))
				.build();

		var resultSet = getResultSet(query, modelSeed);
		assertThat(resultSet.get(Tuple.of(0)), is(UpperCardinalities.UNBOUNDED));
		assertThat(resultSet.get(Tuple.of(1)), is(UpperCardinalities.atMost(17)));
		assertThat(resultSet.get(Tuple.of(2)), is(UpperCardinalities.atMost(9)));
		assertThat(resultSet.get(Tuple.of(3)), is(UpperCardinalities.ZERO));
	}

	private static <T> ResultSet<T> getResultSet(Query<T> query, ModelSeed modelSeed) {
		var personStorage = Symbol.of("Person", 1, TruthValue.class, TruthValue.FALSE);
		var friendStorage = Symbol.of("friend", 2, TruthValue.class, TruthValue.FALSE);

		var store = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder()
						.query(query))
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(PartialRelationTranslator.of(person)
						.symbol(personStorage))
				.with(PartialRelationTranslator.of(friend)
						.symbol(friendStorage))
				.build();

		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);
		return model.getAdapter(ModelQueryAdapter.class).getResultSet(query);
	}
}
