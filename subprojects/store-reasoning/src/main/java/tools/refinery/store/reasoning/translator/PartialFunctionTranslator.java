package tools.refinery.store.reasoning.translator;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.*;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.interpretation.*;
import tools.refinery.store.reasoning.refinement.ConcreteSymbolRefiner;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.refinement.StorageRefiner;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;

public final class PartialFunctionTranslator<A extends AbstractValue<A, C>, C>
		extends PartialSymbolTranslator<A, C> {

	private final PartialFunction<A, C> partialFunction;
	private PartialFunctionRewriter<A, C> rewriter;
	private FunctionalQuery<A> partial;
	private FunctionalQuery<A> candidate;

	PartialFunctionTranslator(PartialFunction<A, C> partialFunction) {
		super(partialFunction);
		this.partialFunction = partialFunction;
	}

	public PartialFunction<A, C> getPartialFunction() {
		return partialFunction;
	}

	@Override
	public PartialFunctionTranslator<A, C> symbol(AnySymbol storageSymbol) {
		super.symbol(storageSymbol);
		return this;
	}

	@Override
	public <T> PartialFunctionTranslator<A, C> symbol(Symbol<T> storageSymbol,
													  StorageRefiner.Factory<T> storageRefiner) {
		super.symbol(storageSymbol, storageRefiner);
		return this;
	}

	@Override
	public PartialFunctionTranslator<A, C> interpretation(
			PartialInterpretation.Factory<A, C> interpretationFactory) {
		super.interpretation(interpretationFactory);
		return this;
	}

	@Override
	public PartialFunctionTranslator<A, C> refiner(
			PartialInterpretationRefiner.Factory<A, C> interpretationRefiner) {
		super.refiner(interpretationRefiner);
		return this;
	}

	public PartialFunctionTranslator<A, C> rewriter(PartialFunctionRewriter<A, C> rewriter) {
		checkNotConfigured();
		if (this.rewriter != null) {
			throw new IllegalArgumentException("Rewriter was already set");
		}
		this.rewriter = rewriter;
		return this;
	}

	public PartialFunctionTranslator<A, C> partial(FunctionalQuery<A> partial) {
		checkNotConfigured();
		if (this.partial != null) {
			throw new IllegalArgumentException("May query was already set");
		}
		this.partial = partial;
		return this;
	}

	public PartialFunctionTranslator<A, C> candidate(FunctionalQuery<A> candidate) {
		checkNotConfigured();
		if (this.candidate != null) {
			throw new IllegalArgumentException("Candidate must query was already set");
		}
		this.candidate = candidate;
		return this;
	}

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		createFallbackInterpretation();
		createFallbackRefiner();
		createFallbackRewriter();
		super.doConfigure(storeBuilder);
	}

	private void createFallbackInterpretation() {
		if (interpretationFactory == null && partial != null && candidate != null) {
			interpretationFactory = new QueryBasedFunctionInterpretationFactory<>(partial, candidate,
					partialFunction.abstractDomain());
		}
	}

	private void createFallbackRefiner() {
		if (interpretationRefiner == null && storageSymbol != null &&
				storageSymbol.valueType().equals(partialFunction.abstractDomain().abstractType())) {
			// We have just checked the value type of {@code storageSymbol}.
			@SuppressWarnings("unchecked")
			var symbol = (Symbol<A>) storageSymbol;
			interpretationRefiner = ConcreteSymbolRefiner.of(symbol);
		}
	}

	private void createFallbackRewriter() {
		if (rewriter == null && partial != null && candidate != null) {
			rewriter = new QueryBasedFunctionRewriter<>(partial, candidate, partialFunction.abstractDomain());
		}
	}

	public PartialFunctionRewriter<A, C> getRewriter() {
		checkConfigured();
		return rewriter;
	}

	public static <A extends AbstractValue<A, C>, C> PartialFunctionTranslator<A, C> of(PartialFunction<A, C> partialFunction) {
		return new PartialFunctionTranslator<>(partialFunction);
	}
}
