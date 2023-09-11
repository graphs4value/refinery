/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import org.jetbrains.annotations.NotNull;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.PartialModelInitializer;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.tuple.Tuple;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;

class MultiObjectInitializer implements PartialModelInitializer {
	private final Symbol<CardinalityInterval> countSymbol;

	public MultiObjectInitializer(Symbol<CardinalityInterval> countSymbol) {
		this.countSymbol = countSymbol;
	}

	@Override
	public void initialize(Model model, ModelSeed modelSeed) {
		var intervals = initializeIntervals(model, modelSeed);
		initializeExists(intervals, model, modelSeed);
		initializeEquals(intervals, model, modelSeed);
		var countInterpretation = model.getInterpretation(countSymbol);
		var uniqueTable = new HashMap<CardinalityInterval, CardinalityInterval>();
		for (int i = 0; i < intervals.length; i++) {
			var interval = intervals[i];
			if (interval.isEmpty()) {
				throw new TranslationException(ReasoningAdapter.EXISTS_SYMBOL,
						"Inconsistent existence or equality for node " + i);
			}
			var uniqueInterval = uniqueTable.computeIfAbsent(intervals[i], Function.identity());
			countInterpretation.put(Tuple.of(i), uniqueInterval);
		}
	}

	@NotNull
	private CardinalityInterval[] initializeIntervals(Model model, ModelSeed modelSeed) {
		var intervals = new CardinalityInterval[modelSeed.getNodeCount()];
		if (modelSeed.containsSeed(MultiObjectTranslator.COUNT_SYMBOL)) {
			Arrays.fill(intervals, CardinalityIntervals.ONE);
			var cursor = modelSeed.getCursor(MultiObjectTranslator.COUNT_SYMBOL, CardinalityIntervals.ONE);
			while (cursor.move()) {
				model.checkCancelled();
				int i = cursor.getKey().get(0);
				checkNodeId(intervals, i);
				intervals[i] = cursor.getValue();
			}
		} else {
			Arrays.fill(intervals, CardinalityIntervals.SET);
			if (!modelSeed.containsSeed(ReasoningAdapter.EXISTS_SYMBOL) ||
					!modelSeed.containsSeed(ReasoningAdapter.EQUALS_SYMBOL)) {
				throw new TranslationException(MultiObjectTranslator.COUNT_SYMBOL,
						"Seed for %s and %s is required if there is no seed for %s".formatted(
								ReasoningAdapter.EXISTS_SYMBOL, ReasoningAdapter.EQUALS_SYMBOL,
								MultiObjectTranslator.COUNT_SYMBOL));
			}
		}
		return intervals;
	}

	private void initializeExists(CardinalityInterval[] intervals, Model model, ModelSeed modelSeed) {
		if (!modelSeed.containsSeed(ReasoningAdapter.EXISTS_SYMBOL)) {
			return;
		}
		var cursor = modelSeed.getCursor(ReasoningAdapter.EXISTS_SYMBOL, TruthValue.UNKNOWN);
		while (cursor.move()) {
			model.checkCancelled();
			int i = cursor.getKey().get(0);
			checkNodeId(intervals, i);
			switch (cursor.getValue()) {
			case TRUE -> intervals[i] = intervals[i].meet(CardinalityIntervals.SOME);
			case FALSE -> intervals[i] = intervals[i].meet(CardinalityIntervals.NONE);
			case ERROR -> throw new TranslationException(ReasoningAdapter.EXISTS_SYMBOL,
					"Inconsistent existence for node " + i);
			default -> throw new TranslationException(ReasoningAdapter.EXISTS_SYMBOL,
					"Invalid existence truth value %s for node %d".formatted(cursor.getValue(), i));
			}
		}
	}

	private void initializeEquals(CardinalityInterval[] intervals, Model model, ModelSeed modelSeed) {
		if (!modelSeed.containsSeed(ReasoningAdapter.EQUALS_SYMBOL)) {
			return;
		}
		var seed = modelSeed.getSeed(ReasoningAdapter.EQUALS_SYMBOL);
		var cursor = seed.getCursor(TruthValue.FALSE, modelSeed.getNodeCount());
		while (cursor.move()) {
			model.checkCancelled();
			var key = cursor.getKey();
			int i = key.get(0);
			int otherIndex = key.get(1);
			if (i != otherIndex) {
				throw new TranslationException(ReasoningAdapter.EQUALS_SYMBOL,
						"Off-diagonal equivalence (%d, %d) is not permitted".formatted(i, otherIndex));
			}
			checkNodeId(intervals, i);
			switch (cursor.getValue()) {
			case TRUE -> intervals[i] = intervals[i].meet(CardinalityIntervals.LONE);
			case UNKNOWN -> {
				// Nothing do to, {@code intervals} is initialized with unknown equality.
			}
			case ERROR -> throw new TranslationException(ReasoningAdapter.EQUALS_SYMBOL,
					"Inconsistent equality for node " + i);
			default -> throw new TranslationException(ReasoningAdapter.EQUALS_SYMBOL,
					"Invalid equality truth value %s for node %d".formatted(cursor.getValue(), i));
			}
		}
		for (int i = 0; i < intervals.length; i++) {
			model.checkCancelled();
			if (seed.get(Tuple.of(i, i)) == TruthValue.FALSE) {
				throw new TranslationException(ReasoningAdapter.EQUALS_SYMBOL, "Inconsistent equality for node " + i);
			}
		}
	}

	private void checkNodeId(CardinalityInterval[] intervals, int nodeId) {
		if (nodeId < 0 || nodeId >= intervals.length) {
			throw new IllegalArgumentException("Expected node id %d to be lower than model size %d"
					.formatted(nodeId, intervals.length));
		}
	}
}
