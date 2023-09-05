/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.internal;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.statecoding.StateCodeCalculator;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.statecoding.StateCoderResult;

public class StateCoderAdapterImpl implements StateCoderAdapter {
	final ModelStoreAdapter storeAdapter;
	final Model model;
	final StateCodeCalculator calculator;

	StateCoderAdapterImpl(ModelStoreAdapter storeAdapter, StateCodeCalculator calculator,  Model model) {
		this.storeAdapter = storeAdapter;
		this.model = model;
		this.calculator = calculator;
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
	public StateCoderResult calculateStateCode() {
		return calculator.calculateCodes();
	}
}
