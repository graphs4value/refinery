/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.dse.transition.objectives.Objectives;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.dnf.FunctionalQuery;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.dnf.RelationalQuery;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.refinement.DefaultStorageRefiner;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.refinement.PartialModelInitializer;
import tools.refinery.store.reasoning.refinement.StorageRefiner;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.translator.AnyPartialSymbolTranslator;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCoderBuilder;

import java.util.*;

public class ReasoningBuilderImpl extends AbstractModelAdapterBuilder<ReasoningStoreAdapterImpl>
		implements ReasoningBuilder {
	private final DnfLifter lifter = new DnfLifter();
	private final PartialQueryRewriter queryRewriter = new PartialQueryRewriter(lifter);
	private Set<Concreteness> requiredInterpretations = Set.of(Concreteness.values());
	private final Map<AnyPartialSymbol, AnyPartialSymbolTranslator> translators = new LinkedHashMap<>();
	private final Map<AnyPartialSymbol, PartialInterpretation.Factory<?, ?>> symbolInterpreters =
			new LinkedHashMap<>();
	private final Map<AnyPartialSymbol, PartialInterpretationRefiner.Factory<?, ?>> symbolRefiners =
			new LinkedHashMap<>();
	private final Map<AnySymbol, StorageRefiner.Factory<?>> registeredStorageRefiners = new LinkedHashMap<>();
	private final List<PartialModelInitializer> initializers = new ArrayList<>();
	private final List<Objective> objectives = new ArrayList<>();

	@Override
	public ReasoningBuilder requiredInterpretations(Collection<Concreteness> requiredInterpretations) {
		this.requiredInterpretations = Set.copyOf(requiredInterpretations);
		return this;
	}

	@Override
	public ReasoningBuilder partialSymbol(AnyPartialSymbolTranslator translator) {
		var partialSymbol = translator.getPartialSymbol();
		var oldConfiguration = translators.put(partialSymbol, translator);
		if (oldConfiguration != null && oldConfiguration != translator) {
			throw new IllegalArgumentException("Duplicate configuration for symbol: " + partialSymbol);
		}
		return this;
	}

	@Override
	public <T> ReasoningBuilder storageRefiner(Symbol<T> symbol, StorageRefiner.Factory<T> refiner) {
		checkNotConfigured();
		if (registeredStorageRefiners.put(symbol, refiner) != null) {
			throw new IllegalArgumentException("Duplicate representation refiner for symbol: " + symbol);
		}
		return this;
	}

	@Override
	public ReasoningBuilder initializer(PartialModelInitializer initializer) {
		checkNotConfigured();
		initializers.add(initializer);
		return this;
	}

	@Override
	public ReasoningBuilder objective(Objective objective) {
		checkNotConfigured();
		objectives.add(objective);
		return this;
	}

	@Override
	public <T> Query<T> lift(Modality modality, Concreteness concreteness, Query<T> query) {
		return lifter.lift(modality, concreteness, query);
	}

	@Override
	public RelationalQuery lift(Modality modality, Concreteness concreteness, RelationalQuery query) {
		return lifter.lift(modality, concreteness, query);
	}

	@Override
	public <T> FunctionalQuery<T> lift(Modality modality, Concreteness concreteness, FunctionalQuery<T> query) {
		return lifter.lift(modality, concreteness, query);
	}

	@Override
	public Dnf lift(Modality modality, Concreteness concreteness, Dnf dnf) {
		return lifter.lift(modality, concreteness, dnf);
	}

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		storeBuilder.symbols(ReasoningAdapterImpl.NODE_COUNT_SYMBOL);
		storeBuilder.tryGetAdapter(StateCoderBuilder.class)
				.ifPresent(stateCoderBuilder -> stateCoderBuilder.exclude(ReasoningAdapterImpl.NODE_COUNT_SYMBOL));
		for (var translator : translators.values()) {
			translator.configure(storeBuilder);
			if (translator instanceof PartialRelationTranslator relationConfiguration) {
				doConfigure(storeBuilder, relationConfiguration);
			} else {
				throw new IllegalArgumentException("Unknown partial symbol translator %s for partial symbol %s"
						.formatted(translator, translator.getPartialSymbol()));
			}
		}
		storeBuilder.symbols(registeredStorageRefiners.keySet());
		var queryBuilder = storeBuilder.getAdapter(ModelQueryBuilder.class);
		queryBuilder.rewriter(queryRewriter);
		if (!objectives.isEmpty()) {
			storeBuilder.tryGetAdapter(DesignSpaceExplorationBuilder.class)
					.ifPresent(dseBuilder -> dseBuilder.objective(Objectives.sum(objectives)));
		}
	}

	private void doConfigure(ModelStoreBuilder storeBuilder, PartialRelationTranslator relationConfiguration) {
		var partialRelation = relationConfiguration.getPartialRelation();
		queryRewriter.addRelationRewriter(partialRelation, relationConfiguration.getRewriter());
		var interpretationFactory = relationConfiguration.getInterpretationFactory();
		interpretationFactory.configure(storeBuilder, requiredInterpretations);
		symbolInterpreters.put(partialRelation, interpretationFactory);
		var refiner = relationConfiguration.getInterpretationRefiner();
		if (refiner != null) {
			symbolRefiners.put(partialRelation, refiner);
		}
	}

	@Override
	public ReasoningStoreAdapterImpl doBuild(ModelStore store) {
		return new ReasoningStoreAdapterImpl(store, requiredInterpretations,
				Collections.unmodifiableMap(symbolInterpreters), Collections.unmodifiableMap(symbolRefiners),
				getStorageRefiners(store), Collections.unmodifiableList(initializers));
	}

	private Map<AnySymbol, StorageRefiner.Factory<?>> getStorageRefiners(ModelStore store) {
		var symbols = store.getSymbols();
		var storageRefiners = new LinkedHashMap<AnySymbol, StorageRefiner.Factory<?>>(symbols.size());
		for (var symbol : symbols) {
			var refiner = registeredStorageRefiners.remove(symbol);
			if (refiner == null) {
				if (symbol.arity() == 0) {
					// Arity-0 symbols don't need a default refiner, because they are unaffected by object
					// creation/removal. Only a custom refiner ({@code refiner != null}) might need to update them.
					continue;
				}
				// By default, copy over all affected tuples on object creation and remove all affected tuples on
				// object removal.
				refiner = DefaultStorageRefiner.factory();
			}
			storageRefiners.put(symbol, refiner);
		}
		if (!registeredStorageRefiners.isEmpty()) {
			throw new IllegalArgumentException("Unused storage refiners: " + registeredStorageRefiners.keySet());
		}
		return Collections.unmodifiableMap(storageRefiners);
	}
}
