/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.internal;

import org.jetbrains.annotations.Nullable;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.AnyPartialInterpretation;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.refinement.AnyPartialInterpretationRefiner;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.refinement.StorageRefiner;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

import java.util.HashMap;
import java.util.Map;

class ReasoningAdapterImpl implements ReasoningAdapter {
	static final Symbol<Integer> NODE_COUNT_SYMBOL = Symbol.of("MODEL_SIZE", 0, Integer.class, 0);
	private final Model model;
	private final ReasoningStoreAdapterImpl storeAdapter;
	private final Map<AnyPartialSymbol, AnyPartialInterpretation>[] partialInterpretations;
	private final Map<AnyPartialSymbol, AnyPartialInterpretationRefiner> refiners;
	private final StorageRefiner[] storageRefiners;
	private final Interpretation<Integer> nodeCountInterpretation;
	private final Interpretation<CardinalityInterval> countInterpretation;

	ReasoningAdapterImpl(Model model, ReasoningStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;

		int concretenessLength = Concreteness.values().length;
		// Creation of a generic array.
		@SuppressWarnings({"unchecked", "squid:S1905"})
		var interpretationsArray = (Map<AnyPartialSymbol, AnyPartialInterpretation>[]) new Map[concretenessLength];
		partialInterpretations = interpretationsArray;
		createPartialInterpretations();

		var refinerFactories = storeAdapter.getSymbolRefiners();
		refiners = new HashMap<>(refinerFactories.size());
		createRefiners();

		storageRefiners = storeAdapter.createStorageRefiner(model);

		nodeCountInterpretation = model.getInterpretation(NODE_COUNT_SYMBOL);
		if (model.getStore().getSymbols().contains(MultiObjectTranslator.COUNT_STORAGE)) {
			countInterpretation = model.getInterpretation(MultiObjectTranslator.COUNT_STORAGE);
		} else {
			countInterpretation = null;
		}
	}

	private void createPartialInterpretations() {
		var supportedInterpretations = storeAdapter.getSupportedInterpretations();
		int concretenessLength = Concreteness.values().length;
		var interpretationFactories = storeAdapter.getSymbolInterpreters();
		for (int i = 0; i < concretenessLength; i++) {
			var concreteness = Concreteness.values()[i];
			if (supportedInterpretations.contains(concreteness)) {
				partialInterpretations[i] = new HashMap<>(interpretationFactories.size());
			}
		}
		// Create the partial interpretations in order so that factories may refer to interpretations of symbols
		// preceding them in the ordered {@code interpretationFactories} map, e.g., for opposite interpretations.
		for (var entry : interpretationFactories.entrySet()) {
			var partialSymbol = entry.getKey();
			var factory = entry.getValue();
			for (int i = 0; i < concretenessLength; i++) {
				if (partialInterpretations[i] != null) {
					var concreteness = Concreteness.values()[i];
					var interpretation = createPartialInterpretation(concreteness, factory, partialSymbol);
					partialInterpretations[i].put(partialSymbol, interpretation);
				}
			}
		}
	}

	private <A, C> PartialInterpretation<A, C> createPartialInterpretation(
			Concreteness concreteness, PartialInterpretation.Factory<A, C> interpreter, AnyPartialSymbol symbol) {
		// The builder only allows well-typed assignment of interpreters to symbols.
		@SuppressWarnings("unchecked")
		var typedSymbol = (PartialSymbol<A, C>) symbol;
		return interpreter.create(this, concreteness, typedSymbol);
	}

	private void createRefiners() {
		var refinerFactories = storeAdapter.getSymbolRefiners();
		// Create the partial interpretations refiners in order so that factories may refer to refiners of symbols
		// preceding them in the ordered {@code interpretationFactories} map, e.g., for opposite interpretations.
		for (var entry : refinerFactories.entrySet()) {
			var partialSymbol = entry.getKey();
			var factory = entry.getValue();
			var refiner = createRefiner(factory, partialSymbol);
			refiners.put(partialSymbol, refiner);
		}
	}

	private <A, C> PartialInterpretationRefiner<A, C> createRefiner(
			PartialInterpretationRefiner.Factory<A, C> factory, AnyPartialSymbol symbol) {
		// The builder only allows well-typed assignment of interpreters to symbols.
		@SuppressWarnings("unchecked")
		var typedSymbol = (PartialSymbol<A, C>) symbol;
		return factory.create(this, typedSymbol);
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public ReasoningStoreAdapterImpl getStoreAdapter() {
		return storeAdapter;
	}

	@Override
	public <A, C> PartialInterpretation<A, C> getPartialInterpretation(Concreteness concreteness,
																	   PartialSymbol<A, C> partialSymbol) {
		var map = partialInterpretations[concreteness.ordinal()];
		if (map == null) {
			throw new IllegalArgumentException("No interpretation for concreteness: " + concreteness);
		}
		var interpretation = map.get(partialSymbol);
		if (interpretation == null) {
			throw new IllegalArgumentException("No interpretation for partial symbol: " + partialSymbol);
		}
		// The builder only allows well-typed assignment of interpreters to symbols.
		@SuppressWarnings("unchecked")
		var typedInterpretation = (PartialInterpretation<A, C>) interpretation;
		return typedInterpretation;
	}

	@Override
	public <A, C> PartialInterpretationRefiner<A, C> getRefiner(PartialSymbol<A, C> partialSymbol) {
		var refiner = refiners.get(partialSymbol);
		if (refiner == null) {
			throw new IllegalArgumentException("No refiner for partial symbol: " + partialSymbol);
		}
		// The builder only allows well-typed assignment of refiners to symbols.
		@SuppressWarnings("unchecked")
		var typedRefiner = (PartialInterpretationRefiner<A, C>) refiner;
		return typedRefiner;
	}

	@Override
	@Nullable
	public Tuple1 split(int parentNode) {
		int newNodeId = nodeCountInterpretation.get(Tuple.of());
		nodeCountInterpretation.put(Tuple.of(), newNodeId + 1);
		// Avoid creating an iterator object.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < storageRefiners.length; i++) {
			if (!storageRefiners[i].split(parentNode, newNodeId)) {
				return null;
			}
		}
		return Tuple.of(newNodeId);
	}

	@Override
	public @Nullable Tuple1 focus(int parentObject) {
		if (countInterpretation == null) {
			throw new IllegalStateException("Cannot focus without " + MultiObjectTranslator.class.getSimpleName());
		}
		var tuple = Tuple.of(parentObject);
		var count = countInterpretation.get(tuple);
		if (CardinalityIntervals.ONE.equals(count)) {
			return tuple;
		}
		if (CardinalityIntervals.LONE.equals(count)) {
			countInterpretation.put(tuple, CardinalityIntervals.ONE);
			return tuple;
		}
		if (CardinalityIntervals.NONE.equals(count)) {
			return null;
		}
		return split(parentObject);
	}

	@Override
	public boolean cleanup(int nodeToDelete) {
		// Avoid creating an iterator object.
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0; i < storageRefiners.length; i++) {
			if (!storageRefiners[i].cleanup(nodeToDelete)) {
				return false;
			}
		}
		int currentModelSize = nodeCountInterpretation.get(Tuple.of());
		if (nodeToDelete == currentModelSize - 1) {
			nodeCountInterpretation.put(Tuple.of(), nodeToDelete);
		}
		return true;
	}

	@Override
	public int getNodeCount() {
		Integer nodeCount = nodeCountInterpretation.get(Tuple.of());
		return nodeCount == null ? 0 : nodeCount;
	}
}
