package tools.refinery.store.reasoning;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.query.view.ForbiddenView;
import tools.refinery.store.query.view.FunctionView;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.ReasoningAdapter.EQUALS_SYMBOL;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;

class AttributeExampleTest
{
	sealed interface Bound
	{
		// left <= right (other is right!)
		boolean lessThanOrEquals(Bound other);
		// left >= right (other is right!)
		boolean greaterThanOrEquals(Bound other);
		boolean equals(Bound other);
	}

	enum InfiniteBound implements Bound
	{
		POSITIVE_INFINITY
		{
			@Override
			public boolean lessThanOrEquals(Bound other)
			{
				return this == other;
			}

			@Override
			public boolean greaterThanOrEquals(Bound other)
			{
				return true;
			}

			@Override
			public boolean equals(Bound other)
			{
				return this == other;
			}
		},
		NEGATIVE_INFINITY
		{
			@Override
			public boolean lessThanOrEquals(Bound other)
			{
				return true;
			}

			@Override
			public boolean greaterThanOrEquals(Bound other)
			{
				return this == other;
			}

			@Override
			public boolean equals(Bound other)
			{
				return this == other;
			}
		}
	}

	record FiniteBound(int value) implements Bound
	{
		// left <= right (other is right!)
		@Override
		public boolean lessThanOrEquals(Bound other)
		{
			return switch (other)
			{
				case InfiniteBound.POSITIVE_INFINITY -> true;
				case InfiniteBound.NEGATIVE_INFINITY -> false;
				case FiniteBound(int otherValue) -> value <= otherValue;
			};
		}

		// left >= right (other is right!)
		@Override
		public boolean greaterThanOrEquals(Bound other)
		{
			return switch (other)
			{
				case InfiniteBound.POSITIVE_INFINITY -> false;
				case InfiniteBound.NEGATIVE_INFINITY -> true;
				case FiniteBound(int otherValue) -> value >= otherValue;
			};
		}

		@Override
		public boolean equals(Bound other)
		{
			return switch (other)
			{
				case InfiniteBound.POSITIVE_INFINITY, InfiniteBound.NEGATIVE_INFINITY -> false;
				case FiniteBound(int otherValue) -> value == otherValue;
			};
		}
	}

	record Interval(@NotNull Bound lowerBound, @NotNull Bound upperBound)
	{
		public static final Interval UNKNOWN = new Interval(InfiniteBound.NEGATIVE_INFINITY, InfiniteBound.POSITIVE_INFINITY);
		public static final Interval ERROR = new Interval(InfiniteBound.POSITIVE_INFINITY, InfiniteBound.NEGATIVE_INFINITY);

		static Interval of(int value)
		{
			var bound = new FiniteBound(value);
			return new Interval(bound, bound);
		}

		static Interval of(int value1, int value2)
		{
			var bound1 = new FiniteBound(value1);
			var bound2 = new FiniteBound(value2);
			return new Interval(bound1, bound2);
		}

		public boolean isSingleton()
		{
			return lowerBound.equals(upperBound);
		}
	}

	// left must <= right
	static class IntervalMustLessThanOrEquals extends BinaryTerm<Boolean, Interval, Interval>
	{
		protected IntervalMustLessThanOrEquals(Term<Interval> left, Term<Interval> right)
		{
			super(Boolean.class, Interval.class, Interval.class, left, right);
		}

		@Override
		protected Boolean doEvaluate(Interval leftValue, Interval rightValue)
		{
			return leftValue.upperBound().lessThanOrEquals(rightValue.lowerBound());
		}

		@Override
		protected Term<Boolean> constructWithSubTerms(Term<Interval> newLeft, Term<Interval> newRight) {
			return new IntervalMustLessThanOrEquals(newLeft, newRight);
		}
	}

	// left may <= right
	static class IntervalMayLessThanOrEquals extends BinaryTerm<Boolean, Interval, Interval>
	{
		protected IntervalMayLessThanOrEquals(Term<Interval> left, Term<Interval> right)
		{
			super(Boolean.class, Interval.class, Interval.class, left, right);
		}

		@Override
		protected Boolean doEvaluate(Interval leftValue, Interval rightValue)
		{
			return leftValue.lowerBound().lessThanOrEquals(rightValue.upperBound());
		}

		@Override
		protected Term<Boolean> constructWithSubTerms(Term<Interval> newLeft, Term<Interval> newRight) {
			return new IntervalMayLessThanOrEquals(newLeft, newRight);
		}
	}

	// left must >= right
	static class IntervalMustGreaterThanOrEquals extends BinaryTerm<Boolean, Interval, Interval>
	{
		protected IntervalMustGreaterThanOrEquals(Term<Interval> left, Term<Interval> right)
		{
			super(Boolean.class, Interval.class, Interval.class, left, right);
		}

		@Override
		protected Boolean doEvaluate(Interval leftValue, Interval rightValue)
		{
			return leftValue.lowerBound().greaterThanOrEquals(rightValue.upperBound());
		}

		@Override
		protected Term<Boolean> constructWithSubTerms(Term<Interval> newLeft, Term<Interval> newRight) {
			return new IntervalMustGreaterThanOrEquals(newLeft, newRight);
		}
	}

	// left may >= right
	static class IntervalMayGreaterThanOrEquals extends BinaryTerm<Boolean, Interval, Interval>
	{
		protected IntervalMayGreaterThanOrEquals(Term<Interval> left, Term<Interval> right)
		{
			super(Boolean.class, Interval.class, Interval.class, left, right);
		}

		@Override
		protected Boolean doEvaluate(Interval leftValue, Interval rightValue)
		{
			return leftValue.lowerBound().greaterThanOrEquals(rightValue.upperBound());
		}

		@Override
		protected Term<Boolean> constructWithSubTerms(Term<Interval> newLeft, Term<Interval> newRight) {
			return new IntervalMayGreaterThanOrEquals(newLeft, newRight);
		}
	}

	// left must in right
	static class IntervalMustIn extends BinaryTerm<Boolean, Interval, Interval>
	{
		protected IntervalMustIn(Term<Interval> left, Term<Interval> right)
		{
			super(Boolean.class, Interval.class, Interval.class, left, right);
		}

		@Override
		protected Boolean doEvaluate(Interval leftValue, Interval rightValue)
		{
			return leftValue.lowerBound().greaterThanOrEquals(rightValue.lowerBound()) &&
				leftValue.lowerBound().lessThanOrEquals(rightValue.upperBound()) &&
				leftValue.upperBound().lessThanOrEquals(rightValue.upperBound()) &&
				leftValue.upperBound().greaterThanOrEquals(rightValue.lowerBound());
		}

		@Override
		protected Term<Boolean> constructWithSubTerms(Term<Interval> newLeft, Term<Interval> newRight) {
			return new IntervalMustIn(newLeft, newRight);
		}
	}

	// left may in right
	static class IntervalMayIn extends BinaryTerm<Boolean, Interval, Interval>
	{
		protected IntervalMayIn(Term<Interval> left, Term<Interval> right)
		{
			super(Boolean.class, Interval.class, Interval.class, left, right);
		}

		@Override
		protected Boolean doEvaluate(Interval leftValue, Interval rightValue)
		{
			return (leftValue.lowerBound().greaterThanOrEquals(rightValue.lowerBound()) &&
				leftValue.lowerBound().lessThanOrEquals(rightValue.upperBound())
			) || (
				leftValue.upperBound().lessThanOrEquals(rightValue.upperBound()) &&
				leftValue.upperBound().greaterThanOrEquals(rightValue.lowerBound())
			);
		}

		@Override
		protected Term<Boolean> constructWithSubTerms(Term<Interval> newLeft, Term<Interval> newRight) {
			return new IntervalMayIn(newLeft, newRight);
		}
	}

	// left must == right
	static class IntervalMustEquals extends BinaryTerm<Boolean, Interval, Interval>
	{
		protected IntervalMustEquals(Term<Interval> left, Term<Interval> right)
		{
			super(Boolean.class, Interval.class, Interval.class, left, right);
		}

		@Override
		protected Boolean doEvaluate(Interval leftValue, Interval rightValue)
		{
			return leftValue.isSingleton() && leftValue.equals(rightValue);
		}

		@Override
		protected Term<Boolean> constructWithSubTerms(Term<Interval> newLeft, Term<Interval> newRight) {
			return new IntervalMustEquals(newLeft, newRight);
		}
	}

	// left may == right
	// contains?
	static class IntervalMayEquals extends BinaryTerm<Boolean, Interval, Interval>
	{
		protected IntervalMayEquals(Term<Interval> left, Term<Interval> right)
		{
			super(Boolean.class, Interval.class, Interval.class, left, right);
		}

		@Override
		protected Boolean doEvaluate(Interval leftValue, Interval rightValue)
		{
			return (leftValue.lowerBound().greaterThanOrEquals(rightValue.lowerBound()) &&
				leftValue.lowerBound().lessThanOrEquals(rightValue.upperBound())
			) || (
				leftValue.upperBound().lessThanOrEquals(rightValue.upperBound()) &&
				leftValue.upperBound().greaterThanOrEquals(rightValue.lowerBound())
			);
		}

		@Override
		protected Term<Boolean> constructWithSubTerms(Term<Interval> newLeft, Term<Interval> newRight) {
			return new IntervalMayEquals(newLeft, newRight);
		}
	}

	// left must < right
	static class IntervalMustLessThan extends BinaryTerm<Boolean, Interval, Interval>
	{
		protected IntervalMustLessThan(Term<Interval> left, Term<Interval> right)
		{
			super(Boolean.class, Interval.class, Interval.class, left, right);
		}

		@Override
		protected Boolean doEvaluate(Interval leftValue, Interval rightValue)
		{
			return !leftValue.lowerBound().greaterThanOrEquals(rightValue.lowerBound());
		}

		@Override
		protected Term<Boolean> constructWithSubTerms(Term<Interval> newLeft, Term<Interval> newRight)
		{
			return new IntervalMustLessThan(newLeft, newRight);
		}
	}

	// left may > right
	static class IntervalMayLessThan extends BinaryTerm<Boolean, Interval, Interval>
	{
		protected IntervalMayLessThan(Term<Interval> left, Term<Interval> right)
		{
			super(Boolean.class, Interval.class, Interval.class, left, right);
		}

		@Override
		protected Boolean doEvaluate(Interval leftValue, Interval rightValue)
		{
			return !leftValue.lowerBound().greaterThanOrEquals(rightValue.upperBound());
		}

		@Override
		protected Term<Boolean> constructWithSubTerms(Term<Interval> newLeft, Term<Interval> newRight) {
			return new IntervalMayLessThan(newLeft, newRight);
		}
	}

	static Term<Interval> constant(Interval interval)
	{
		return new ConstantTerm<>(Interval.class, interval);
	}

	private final PartialRelation person = new PartialRelation("Person", 1);
	private final Symbol<TruthValue> personStorage = Symbol.of("Person", 1, TruthValue.class, TruthValue.UNKNOWN);
	private final PartialRelation vehicle = new PartialRelation("Vehicle", 1);
	private final Symbol<TruthValue> vehicleStorage = Symbol.of("Vehicle", 1, TruthValue.class, TruthValue.UNKNOWN);
	private final Symbol<Interval> ageStorage = Symbol.of("age", 1, Interval.class, Interval.UNKNOWN);
	private final FunctionView<Interval> ageView = new FunctionView<>(ageStorage);
	private final PartialRelation adult = new PartialRelation("adult", 1);
	private final PartialRelation canPlayBoardgames = new PartialRelation("canPlayBoardgames", 1);
	// young -> old
	private final PartialRelation younger = new PartialRelation("younger", 2);

	@Test
	void attributeExample()
	{
		var age = Query.of("age", Interval.class, (builder, p1, output) -> builder
			.clause(
				may(person.call(p1)),
				output.assign(ageView.leftJoin(Interval.UNKNOWN, p1))
			));

		var store = ModelStore.builder()
			.symbol(ageStorage)
			.with(QueryInterpreterAdapter.builder()
				.query(age))
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
			.with(ReasoningAdapter.builder()
					.requiredInterpretations(Concreteness.PARTIAL))
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
			.with(PartialRelationTranslator.of(adult)
					.must(Query.of("adult#must", (builder, p1) -> builder
							.clause(Interval.class, a -> List.of(
									must(person.call(p1)),
									a.assign(age.leftJoin(Interval.ERROR, p1)),
									check(new IntervalMustLessThanOrEquals(constant(Interval.of(18)), a))
							))
					))
					.may(Query.of("adult#may", (builder, p1) -> builder
							.clause(Interval.class, a -> List.of(
									may(person.call(p1)),
									a.assign(age.leftJoin(Interval.ERROR, p1)),
									check(new IntervalMayLessThanOrEquals(constant(Interval.of(18)), a))
							))
					))
			)
			.with(PartialRelationTranslator.of(canPlayBoardgames)
					.must(Query.of("canPlayBoardgames#must", (builder, p1) -> builder
							.clause(Interval.class, a -> List.of(
									must(person.call(p1)),
									a.assign(age.leftJoin(Interval.ERROR, p1)),
									check(new IntervalMustIn(a,constant(Interval.of(6,99))))
							))
					))
					.may(Query.of("canPlayBoardgames#may", (builder, p1) -> builder
							.clause(Interval.class, a -> List.of(
									may(person.call(p1)),
									a.assign(age.leftJoin(Interval.ERROR, p1)),
									check(new IntervalMayIn(a,constant(Interval.of(6,99))))
							))
					))
			)
			.with(PartialRelationTranslator.of(younger)
					.must(Query.of("younger#must", (builder, p1, p2) -> builder
							.clause(Interval.class, Interval.class, (a,b) -> List.of(
									must(person.call(p1)),
									must(person.call(p2)),
									not(may(EQUALS_SYMBOL.call(p1, p2))),
									a.assign(age.leftJoin(Interval.ERROR, p1)),
									b.assign(age.leftJoin(Interval.ERROR, p2)),
									check(new IntervalMustLessThan(a, b))
							))
					))
					.may(Query.of("younger#may", (builder, p1, p2) -> builder
							.clause(Interval.class, Interval.class, (a,b) -> List.of(
									may(person.call(p1)),
									may(person.call(p2)),
									not(must(EQUALS_SYMBOL.call(p1, p2))),
									a.assign(age.leftJoin(Interval.ERROR, p1)),
									b.assign(age.leftJoin(Interval.ERROR, p2)),
									check(new IntervalMayLessThan(a, b))
							))
					))
			)
			.build();

		var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(ModelSeed.builder(9)
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
						.put(Tuple.of(6),TruthValue.TRUE)
						.put(Tuple.of(7), TruthValue.TRUE)
						.put(Tuple.of(8), TruthValue.TRUE)
				)
				.seed(vehicle, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(3), TruthValue.TRUE)
				)
				.build());

		var queryEngine = model.getAdapter(ModelQueryAdapter.class);
		var ageResultSet = queryEngine.getResultSet(age);
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		var personInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, person);
		var vehicleInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, vehicle);
		var adultInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, adult);
		var canPlayBoardgamesInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, canPlayBoardgames);
		var youngerInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, younger);

		var ageStorageInterpretation = model.getInterpretation(ageStorage);
		ageStorageInterpretation.put(Tuple.of(1), Interval.of(32));
		ageStorageInterpretation.put(Tuple.of(4), Interval.of(16));
		ageStorageInterpretation.put(Tuple.of(8), Interval.of(5));
		ageStorageInterpretation.put(Tuple.of(5), Interval.of(5,10));
		ageStorageInterpretation.put(Tuple.of(6), new Interval(new FiniteBound(6),InfiniteBound.POSITIVE_INFINITY));
		ageStorageInterpretation.put(Tuple.of(7), new Interval(InfiniteBound.NEGATIVE_INFINITY,InfiniteBound.POSITIVE_INFINITY));
		queryEngine.flushChanges();

		assertThat(personInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));
		assertThat(personInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(personInterpretation.get(Tuple.of(2)), is(TruthValue.FALSE));
		assertThat(personInterpretation.get(Tuple.of(3)), is(TruthValue.ERROR));

		assertThat(vehicleInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));
		assertThat(vehicleInterpretation.get(Tuple.of(1)), is(TruthValue.FALSE));
		assertThat(vehicleInterpretation.get(Tuple.of(2)), is(TruthValue.TRUE));
		assertThat(vehicleInterpretation.get(Tuple.of(3)), is(TruthValue.ERROR));

		// 0's age IS unknown
		assertThat(ageResultSet.get(Tuple.of(0)), is(Interval.UNKNOWN));
		assertThat(ageResultSet.get(Tuple.of(1)), is(Interval.of(32)));
		assertThat(ageResultSet.get(Tuple.of(4)), is(Interval.of(16)));
		assertThat(ageResultSet.get(Tuple.of(2)), is(nullValue()));

		assertThat(adultInterpretation.get(Tuple.of(0)), is(TruthValue.UNKNOWN));
		assertThat(adultInterpretation.get(Tuple.of(1)), is(TruthValue.TRUE));
		assertThat(adultInterpretation.get(Tuple.of(2)), is(TruthValue.FALSE));
		assertThat(adultInterpretation.get(Tuple.of(3)), is(TruthValue.ERROR));

		// false because 0 may person (to be younger -> you have to be a person)
		assertThat(youngerInterpretation.get(Tuple.of(1,0)), is(TruthValue.UNKNOWN));

		// error because 3 is a vehicle and a person at the same time (age is error)
		assertThat(youngerInterpretation.get(Tuple.of(1,3)), is(TruthValue.ERROR));
		// false because 2 is not a person
		assertThat(youngerInterpretation.get(Tuple.of(1,2)), is(TruthValue.FALSE));
		// false because 4 is older than 1 (32>16)
		assertThat(youngerInterpretation.get(Tuple.of(1,4)), is(TruthValue.FALSE));
		// true because 4 is younger than 1 (16<32)
		assertThat(youngerInterpretation.get(Tuple.of(4,1)), is(TruthValue.TRUE));
		// unknown because 6 can be older than 5 (11>10)
		assertThat(youngerInterpretation.get(Tuple.of(6,5)), is(TruthValue.UNKNOWN));

		var cursor = youngerInterpretation.getAll();
		while (cursor.move())
			System.out.printf("%s: %s%n", cursor.getKey(), cursor.getValue());

		System.out.println("BOARDGAMES");
		var cursor1 = canPlayBoardgamesInterpretation.getAll();
		while (cursor1.move())
			System.out.printf("%s: %s%n", cursor1.getKey(), cursor1.getValue());

	}
}
