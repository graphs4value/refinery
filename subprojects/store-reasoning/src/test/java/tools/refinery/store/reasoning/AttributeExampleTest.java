package tools.refinery.store.reasoning;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.intinterval.Bound;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueTerms;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.query.view.ForbiddenView;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.attribute.AttributeInfo;
import tools.refinery.store.reasoning.translator.attribute.AttributeTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.logic.term.intinterval.IntIntervalTerms.*;
import static tools.refinery.store.reasoning.ReasoningAdapter.EQUALS_SYMBOL;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;

class AttributeExampleTest {

	private final PartialRelation person = new PartialRelation("Person", 1);
	private final Symbol<TruthValue> personStorage = Symbol.of("Person", 1, TruthValue.class, TruthValue.UNKNOWN);
	private final PartialRelation vehicle = new PartialRelation("Vehicle", 1);
	private final Symbol<TruthValue> vehicleStorage = Symbol.of("Vehicle", 1, TruthValue.class, TruthValue.UNKNOWN);
	private final PartialFunction<IntInterval, Integer> age = new PartialFunction<>("age", 1,
			IntIntervalDomain.INSTANCE);
	private final PartialRelation adult = new PartialRelation("adult", 1);
	private final PartialRelation canPlayBoardgames = new PartialRelation("canPlayBoardgames", 1);
	private final PartialRelation younger = new PartialRelation("younger", 2);

	@Test
	void attributeExample() {
		var store = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder()
						.rule(Rule.of("markAsVehicle", (builder, p1) -> builder
								.clause(
										may(ReasoningAdapter.EXISTS_SYMBOL.call(p1)),
										not(must(vehicle.call(p1))),
										not(may(person.call(p1)))
								)
								.action(
										add(vehicle, p1)
								)))
						.rule(Rule.of("markAsPerson", (builder, p1) -> builder
								.clause(
										may(ReasoningAdapter.EXISTS_SYMBOL.call(p1)),
										not(must(person.call(p1))),
										not(may(vehicle.call(p1)))
								)
								.action(
										add(person, p1)
								)
						))
				)
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(PartialRelationTranslator.of(person)
						.symbol(personStorage)
						.may(Query.of("Person#may", (builder, p1) -> builder.clause(
								may(ReasoningAdapter.EXISTS_SYMBOL.call(p1)),
								not(new ForbiddenView(personStorage).call(p1)),
								not(must(vehicle.call(p1)))
						)))
				)
				.with(PartialRelationTranslator.of(vehicle)
						.symbol(vehicleStorage)
						.may(Query.of("Vehicle#may", (builder, p1) -> builder.clause(
								may(ReasoningAdapter.EXISTS_SYMBOL.call(p1)),
								not(new ForbiddenView(vehicleStorage).call(p1)),
								not(must(person.call(p1)))
						)))
				)
				.with(new AttributeTranslator<>(age, new AttributeInfo(person)))
				.with(PartialRelationTranslator.of(adult)
						.query(Query.of("adult", (builder, p1) -> builder
								.clause(
										person.call(p1),
										partialCheck(greaterEq(age.call(p1), constant(IntInterval.of(18))))
								)
						))
				)
				.with(PartialRelationTranslator.of(canPlayBoardgames)
						.query(Query.of("canPlayBoardgames", (builder, p1) -> builder
								.clause(
										person.call(p1),
										partialCheck(TruthValueTerms.and(
												greaterEq(age.call(p1), constant(IntInterval.of(6))),
												lessEq(age.call(p1), constant(IntInterval.of(99)))
										))
								)
						))
				)
				.with(PartialRelationTranslator.of(younger)
						.query(Query.of("younger", (builder, p1, p2) -> builder
								.clause(
										person.call(p1),
										person.call(p2),
										not(EQUALS_SYMBOL.call(p1, p2)),
										partialCheck(less(age.call(p1), age.call(p2)))
								)
						))
				)
				.build();

		ReasoningAdapter reasoningAdapter;
		try (var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(ModelSeed.builder(9)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
				)
				.seed(person, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(1), TruthValue.TRUE)
						.put(Tuple.of(2), TruthValue.FALSE)
						.put(Tuple.of(3), TruthValue.TRUE)
						.put(Tuple.of(4), TruthValue.TRUE)
						.put(Tuple.of(5), TruthValue.TRUE)
						.put(Tuple.of(6), TruthValue.TRUE)
						.put(Tuple.of(7), TruthValue.TRUE)
						.put(Tuple.of(8), TruthValue.TRUE)
				)
				.seed(vehicle, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(3), TruthValue.TRUE)
				)
				.seed(age, builder -> builder
						.reducedValue(IntInterval.UNKNOWN)
						.put(Tuple.of(1), IntInterval.of(32))
						.put(Tuple.of(4), IntInterval.of(16))
						.put(Tuple.of(8), IntInterval.of(5))
						.put(Tuple.of(5), IntInterval.of(5, 10))
						.put(Tuple.of(6), IntInterval.of(6, Bound.Infinite.POSITIVE_INFINITY))
						.put(Tuple.of(7), IntInterval.of(Bound.Infinite.NEGATIVE_INFINITY,
								Bound.Infinite.POSITIVE_INFINITY))
				)
				.build())) {

			reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		}
		var personInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, person);
		var vehicleInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, vehicle);
		var adultInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, adult);
		var youngerInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, younger);
		var canPlayBoardgamesInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL,
				canPlayBoardgames);
		var ageInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, age);

		assertThat(personInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));
		assertThat(personInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(personInterpretation.get(Tuple.of(2)), is(TruthValue.FALSE));
		assertThat(personInterpretation.get(Tuple.of(3)), is(TruthValue.ERROR));

		assertThat(vehicleInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));
		assertThat(vehicleInterpretation.get(Tuple.of(1)), is(TruthValue.FALSE));
		assertThat(vehicleInterpretation.get(Tuple.of(2)), is(TruthValue.TRUE));
		assertThat(vehicleInterpretation.get(Tuple.of(3)), is(TruthValue.ERROR));

		assertThat(adultInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));
		assertThat(adultInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(adultInterpretation.get(Tuple.of(2)), is(TruthValue.FALSE));
		assertThat(adultInterpretation.get(Tuple.of(3)), is(TruthValue.ERROR));

		assertThat(ageInterpretation.get(Tuple.of(0)), is(IntInterval.UNKNOWN));
		assertThat(ageInterpretation.get(Tuple.of(1)), is(IntInterval.of(32)));
		assertThat(ageInterpretation.get(Tuple.of(4)), is(IntInterval.of(16)));
		assertThat(ageInterpretation.get(Tuple.of(2)), is(IntInterval.ERROR));

		assertThat(adultInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));
		assertThat(adultInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(adultInterpretation.get(Tuple.of(2)), is(TruthValue.FALSE));
		assertThat(adultInterpretation.get(Tuple.of(3)), is(TruthValue.ERROR));

		assertThat(youngerInterpretation.get(Tuple.of(1, 0)), is(TruthValue.UNKNOWN));
		assertThat(youngerInterpretation.get(Tuple.of(1, 3)), is(TruthValue.ERROR));
		assertThat(youngerInterpretation.get(Tuple.of(1, 2)), is(TruthValue.FALSE));
		assertThat(youngerInterpretation.get(Tuple.of(1, 4)), is(TruthValue.FALSE));
		assertThat(youngerInterpretation.get(Tuple.of(4, 1)), is(TruthValue.TRUE));
		assertThat(youngerInterpretation.get(Tuple.of(6, 5)), is(TruthValue.UNKNOWN));

		assertThat(canPlayBoardgamesInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));
		assertThat(canPlayBoardgamesInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(canPlayBoardgamesInterpretation.get(Tuple.of(2)), is(TruthValue.FALSE));
		assertThat(canPlayBoardgamesInterpretation.get(Tuple.of(3)), is(TruthValue.ERROR));
		assertThat(canPlayBoardgamesInterpretation.get(Tuple.of(4)), is(TruthValue.TRUE));
		assertThat(canPlayBoardgamesInterpretation.get(Tuple.of(5)), is(TruthValue.UNKNOWN));
		assertThat(canPlayBoardgamesInterpretation.get(Tuple.of(6)), is(TruthValue.UNKNOWN));
		assertThat(canPlayBoardgamesInterpretation.get(Tuple.of(7)), is(TruthValue.UNKNOWN));
		assertThat(canPlayBoardgamesInterpretation.get(Tuple.of(8)), is(TruthValue.FALSE));

	}

}
