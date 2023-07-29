/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.internal;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.statecoding.neighbourhood.NeighbourhoodCalculator;

import java.util.Collection;
import java.util.List;

public class StateCoderAdapterImpl implements StateCoderAdapter {
	final ModelStoreAdapter storeAdapter;
	final Model model;
	final NeighbourhoodCalculator calculator;

	StateCoderAdapterImpl(ModelStoreAdapter storeAdapter, Model model, Collection<Symbol<?>> symbols) {
		this.storeAdapter = storeAdapter;
		this.model = model;

		List<? extends Interpretation<?>> interpretations = symbols.stream().map(model::getInterpretation).toList();
		calculator = new NeighbourhoodCalculator(interpretations);
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public ModelStoreAdapter getStoreAdapter() {
		return storeAdapter;
	}

	@Override
	public int calculateHashCode() {
		return calculator.calculate();
	}


}
