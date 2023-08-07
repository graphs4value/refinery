/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.internal;

import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCodeCalculatorFactory;
import tools.refinery.store.statecoding.StateCoderBuilder;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;
import tools.refinery.store.statecoding.StateEquivalenceChecker;
import tools.refinery.store.statecoding.neighbourhood.LazyNeighbourhoodCalculatorFactory;
import tools.refinery.store.statecoding.stateequivalence.StateEquivalenceCheckerImpl;
import tools.refinery.store.tuple.Tuple1;

import java.util.*;

public class StateCoderBuilderImpl implements StateCoderBuilder {
	Set<AnySymbol> excluded = new HashSet<>();
	IntHashSet individuals = new IntHashSet();

	StateCodeCalculatorFactory calculator = new LazyNeighbourhoodCalculatorFactory();
	StateEquivalenceChecker checker = new StateEquivalenceCheckerImpl();

	@Override
	public StateCoderBuilder exclude(AnySymbol symbol) {
		excluded.add(symbol);
		return this;
	}

	@Override
	public StateCoderBuilder individual(Tuple1 tuple) {
		individuals.add(tuple.get(0));
		return this;
	}

	@Override
	public StateCoderBuilder stateEquivalenceChecker(StateEquivalenceChecker stateEquivalenceChecker) {
		this.checker = stateEquivalenceChecker;
		return this;
	}

	@Override
	public StateCoderBuilder stateCodeCalculatorFactory(StateCodeCalculatorFactory codeCalculatorFactory) {
		this.calculator = codeCalculatorFactory;
		return this;
	}

	@Override
	public boolean isConfigured() {
		return true;
	}

	@Override
	public void configure(ModelStoreBuilder storeBuilder) {
		// It does not modify the build process
	}

	@Override
	public StateCoderStoreAdapter build(ModelStore store) {
		Set<Symbol<?>> symbols = new LinkedHashSet<>();
		for (AnySymbol symbol : store.getSymbols()) {
			if (!excluded.contains(symbol) && (symbol instanceof Symbol<?> typed)) {
				symbols.add(typed);
			}
		}
		return new StateCoderStoreAdapterImpl(store, calculator, checker, symbols, individuals);
	}
}
