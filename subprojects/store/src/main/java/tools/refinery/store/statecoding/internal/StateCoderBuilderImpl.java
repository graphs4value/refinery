/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.internal;

import org.eclipse.collections.api.factory.primitive.IntSets;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCodeCalculatorFactory;
import tools.refinery.store.statecoding.StateCoderBuilder;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;
import tools.refinery.store.statecoding.StateEquivalenceChecker;
import tools.refinery.store.statecoding.neighbourhood.NeighbourhoodCalculator;
import tools.refinery.store.statecoding.stateequivalence.StateEquivalenceCheckerImpl;
import tools.refinery.store.tuple.Tuple1;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class StateCoderBuilderImpl extends AbstractModelAdapterBuilder<StateCoderStoreAdapter>
		implements StateCoderBuilder {
	private final Set<AnySymbol> excluded = new HashSet<>();
	private final MutableIntSet individuals = IntSets.mutable.empty();
	private StateCodeCalculatorFactory calculator = NeighbourhoodCalculator::new;
	private StateEquivalenceChecker checker = new StateEquivalenceCheckerImpl();

	@Override
	public StateCoderBuilder exclude(AnySymbol symbol) {
		checkNotConfigured();
		excluded.add(symbol);
		return this;
	}

	@Override
	public StateCoderBuilder individual(Tuple1 tuple) {
		checkNotConfigured();
		individuals.add(tuple.get(0));
		return this;
	}

	@Override
	public StateCoderBuilder stateEquivalenceChecker(StateEquivalenceChecker stateEquivalenceChecker) {
		checkNotConfigured();
		this.checker = stateEquivalenceChecker;
		return this;
	}

	@Override
	public StateCoderBuilder stateCodeCalculatorFactory(StateCodeCalculatorFactory codeCalculatorFactory) {
		checkNotConfigured();
		this.calculator = codeCalculatorFactory;
		return this;
	}

	@Override
	protected StateCoderStoreAdapter doBuild(ModelStore store) {
		Set<Symbol<?>> symbols = new LinkedHashSet<>();
		for (AnySymbol symbol : store.getSymbols()) {
			if (!excluded.contains(symbol) && (symbol instanceof Symbol<?> typed)) {
				symbols.add(typed);
			}
		}
		return new StateCoderStoreAdapterImpl(store, calculator, checker, symbols, individuals);
	}
}
