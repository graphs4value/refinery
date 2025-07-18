/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.cardinalityinterval.CardinalityDomain;
import tools.refinery.logic.term.cardinalityinterval.CardinalityInterval;
import tools.refinery.logic.term.int_.IntTerms;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.uppercardinality.UpperCardinalities;
import tools.refinery.logic.term.uppercardinality.UpperCardinality;
import tools.refinery.logic.term.uppercardinality.UpperCardinalityTerms;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.objectives.Criteria;
import tools.refinery.store.dse.transition.objectives.Objectives;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.translator.PartialFunctionTranslator;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.representation.Symbol;

import java.util.List;

import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.term.int_.IntTerms.*;

public class MultiObjectTranslator implements ModelStoreConfiguration {
	public static final Symbol<CardinalityInterval> COUNT_STORAGE = Symbol.of("COUNT", 1, CardinalityInterval.class,
			null);
	public static final AnySymbolView LOWER_CARDINALITY_VIEW = new LowerCardinalityView(COUNT_STORAGE);
	public static final AnySymbolView UPPER_CARDINALITY_VIEW = new UpperCardinalityView(COUNT_STORAGE);
	public static final AnySymbolView MULTI_VIEW = new MultiView(COUNT_STORAGE);
	public static final PartialFunction<CardinalityInterval, Integer> COUNT_SYMBOL = new PartialFunction<>("COUNT", 1,
			CardinalityDomain.INSTANCE);

	private final boolean keepNonExistingObjects;

	public MultiObjectTranslator(boolean keepNonExistingObjects) {
		this.keepNonExistingObjects = keepNonExistingObjects;
	}

	public MultiObjectTranslator() {
		this(true);
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		storeBuilder.symbol(COUNT_STORAGE);

		var aboveLowerBound = Query.of("count#aboveLowerBound", Integer.class, (builder, node, output) -> builder
				.clause(
						MULTI_VIEW.call(node),
						LOWER_CARDINALITY_VIEW.call(node, output),
						check(greater(output, IntTerms.constant(0)))
				));
		var missingCardinality = Query.of("count#missing", Integer.class, (builder, output) -> builder
				.clause(
						output.assign(aboveLowerBound.aggregate(INT_SUM, Variable.of()))
				));

		storeBuilder.with(PartialRelationTranslator.of(ReasoningAdapter.EXISTS_SYMBOL)
				.may(Query.of("exists#may", (builder, p1) -> builder
						.clause(UpperCardinality.class, upper -> List.of(
								UPPER_CARDINALITY_VIEW.call(p1, upper),
								check(UpperCardinalityTerms.greaterEq(upper,
										UpperCardinalityTerms.constant(UpperCardinalities.ONE)))
						))))
				.must(Query.of("exists#must", (builder, p1) -> builder
						.clause(Integer.class, lower -> List.of(
								LOWER_CARDINALITY_VIEW.call(p1, lower),
								check(greaterEq(lower, constant(1)))
						))))
				// Multi-objects which surely exist in the partial view will also exist in the candidate view,
				// but they may have inconsistent {@code COUNT} that refines their {@code COUNT} from the partial view.
				.roundingMode(RoundingMode.PREFER_FALSE)
				.refiner(ExistsRefiner.of(COUNT_STORAGE))
				.exclude(null)
				.accept(Criteria.whenNoMatch(aboveLowerBound))
				.objective(Objectives.value(missingCardinality)));

		storeBuilder.with(PartialRelationTranslator.of(ReasoningAdapter.EQUALS_SYMBOL)
				.rewriter(EqualsRelationRewriter.of(UPPER_CARDINALITY_VIEW))
				.refiner(EqualsRefiner.of(COUNT_STORAGE))
				.exclude(null)
				.accept(null)
				.objective(null));

		storeBuilder.with(PartialFunctionTranslator.of(ReasoningAdapter.COUNT_SYMBOL)
				.partial(Query.of("count#partial", IntInterval.class, (builder, p1, output) -> builder
						.clause(
								new CountIntervalView(COUNT_STORAGE).call(p1, output)
						)))
				.candidate(Query.of("count#candidate", IntInterval.class, (builder, p1, output) -> builder
						.clause(
								new CountIntervalCandidateView(COUNT_STORAGE).call(p1, output)
						)))
				.exclude(null)
				.accept(null)
				.objective(null));

		var reasoningBuilder = storeBuilder.getAdapter(ReasoningBuilder.class);
		reasoningBuilder.initializer(new MultiObjectInitializer(COUNT_STORAGE));
		reasoningBuilder.storageRefiner(COUNT_STORAGE, MultiObjectStorageRefiner::new);

		storeBuilder.tryGetAdapter(PropagationBuilder.class).ifPresent(propagationBuilder ->
				propagationBuilder.propagator(new CleanupPropagator(keepNonExistingObjects, this)));
	}
}
