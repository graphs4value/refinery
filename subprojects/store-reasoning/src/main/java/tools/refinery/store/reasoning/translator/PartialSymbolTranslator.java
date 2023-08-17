/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator;

import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.refinement.PartialModelInitializer;
import tools.refinery.store.reasoning.refinement.StorageRefiner;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.SeedInitializer;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;

import java.util.Set;

@SuppressWarnings("UnusedReturnValue")
public abstract sealed class PartialSymbolTranslator<A, C> implements AnyPartialSymbolTranslator
		permits PartialRelationTranslator {
	private final PartialSymbol<A, C> partialSymbol;
	private boolean configured = false;
	protected PartialInterpretationRefiner.Factory<A, C> interpretationRefiner;
	protected AnySymbol storageSymbol;
	protected StorageRefiner.Factory<?> storageRefiner;
	protected PartialInterpretation.Factory<A, C> interpretationFactory;
	protected PartialModelInitializer initializer;

	PartialSymbolTranslator(PartialSymbol<A, C> partialSymbol) {
		this.partialSymbol = partialSymbol;
	}

	@Override
	public PartialSymbol<A, C> getPartialSymbol() {
		return partialSymbol;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		storeBuilder.getAdapter(ReasoningBuilder.class).partialSymbol(this);
	}

	public boolean isConfigured() {
		return configured;
	}

	protected void checkConfigured() {
		if (!configured) {
			throw new IllegalStateException("Partial symbol was not configured");
		}
	}

	protected void checkNotConfigured() {
		if (configured) {
			throw new IllegalStateException("Partial symbol was already configured");
		}
	}

	public PartialSymbolTranslator<A, C> symbol(AnySymbol storageSymbol) {
		return symbol((Symbol<?>) storageSymbol, null);
	}

	public <T> PartialSymbolTranslator<A, C> symbol(Symbol<T> storageSymbol,
													StorageRefiner.Factory<T> storageRefiner) {
		checkNotConfigured();
		if (this.storageSymbol != null) {
			throw new IllegalStateException("Representation symbol was already set");
		}
		this.storageSymbol = storageSymbol;
		this.storageRefiner = storageRefiner;
		return this;
	}

	public PartialSymbolTranslator<A, C> interpretation(PartialInterpretation.Factory<A, C> interpretationFactory) {
		checkNotConfigured();
		if (this.interpretationFactory != null) {
			throw new IllegalStateException("Interpretation factory was already set");
		}
		this.interpretationFactory = interpretationFactory;
		return this;
	}

	public PartialSymbolTranslator<A, C> refiner(PartialInterpretationRefiner.Factory<A, C> interpretationRefiner) {
		checkNotConfigured();
		if (this.interpretationRefiner != null) {
			throw new IllegalStateException("Interpretation refiner was already set");
		}
		this.interpretationRefiner = interpretationRefiner;
		return this;
	}

	public PartialSymbolTranslator<A, C> initializer(PartialModelInitializer initializer) {
		checkNotConfigured();
		if (this.initializer != null) {
			throw new IllegalStateException("Initializer was already set");
		}
		this.initializer = initializer;
		return this;
	}

	@Override
	public void configure(ModelStoreBuilder storeBuilder) {
		checkNotConfigured();
		doConfigure(storeBuilder);
		configured = true;
	}

	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		if (interpretationFactory == null) {
			throw new IllegalArgumentException("Interpretation factory must be set");
		}
		var reasoningBuilder = storeBuilder.getAdapter(ReasoningBuilder.class);
		if (storageSymbol != null) {
			storeBuilder.symbol(storageSymbol);
			if (storageRefiner != null) {
				registerStorageRefiner(reasoningBuilder, storageRefiner);
			}
		}
		createFallbackInitializer();
		if (initializer != null) {
			reasoningBuilder.initializer(initializer);
		}
	}

	private <T> void registerStorageRefiner(ReasoningBuilder reasoningBuilder, StorageRefiner.Factory<T> factory) {
		// The builder only allows setting a well-typed representation refiner.
		@SuppressWarnings("unchecked")
		var typedStorageSymbol = (Symbol<T>) storageSymbol;
		reasoningBuilder.storageRefiner(typedStorageSymbol, factory);
	}

	private void createFallbackInitializer() {
		if (initializer == null &&
				storageSymbol != null &&
				storageSymbol.valueType().equals(partialSymbol.abstractDomain().abstractType())) {
			// The guard clause makes this safe.
			@SuppressWarnings("unchecked")
			var typedStorageSymbol = (Symbol<A>) storageSymbol;
			initializer = new SeedInitializer<>(typedStorageSymbol, partialSymbol);
		}
	}

	public PartialInterpretation.Factory<A, C> getInterpretationFactory() {
		checkConfigured();
		return interpretationFactory;
	}

	public PartialInterpretationRefiner.Factory<A, C> getInterpretationRefiner() {
		checkConfigured();
		return interpretationRefiner;
	}
}
