/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding.internal;

import org.eclipse.collections.api.set.primitive.IntSet;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCodeCalculatorFactory;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;
import tools.refinery.store.statecoding.StateEquivalenceChecker;

import java.util.Collection;
import java.util.Objects;

public class StateCoderStoreAdapterImpl implements StateCoderStoreAdapter {
	final ModelStore store;
	final Collection<Symbol<?>> symbols;
	final IntSet individuals;

	final StateEquivalenceChecker equivalenceChecker;
	final StateCodeCalculatorFactory codeCalculatorFactory;

	StateCoderStoreAdapterImpl(ModelStore store,
							   StateCodeCalculatorFactory codeCalculatorFactory,
							   StateEquivalenceChecker equivalenceChecker,
							   Collection<Symbol<?>> symbols,
							   IntSet individuals)
	{
		this.codeCalculatorFactory = codeCalculatorFactory;
		this.equivalenceChecker = equivalenceChecker;
		this.store = store;
		this.symbols = symbols;
		this.individuals = individuals;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public StateEquivalenceChecker.EquivalenceResult checkEquivalence(Version v1, Version v2) {
		if (Objects.equals(v1, v2)) {
			return StateEquivalenceChecker.EquivalenceResult.ISOMORPHIC;
		}
		var model1 = this.getStore().createModelForState(v1);
		var model2 = this.getStore().createModelForState(v2);

		var s1 = model1.getAdapter(StateCoderAdapter.class).calculateStateCode();
		var s2 = model2.getAdapter(StateCoderAdapter.class).calculateStateCode();

		if (s1.modelCode() != s2.modelCode()) {
			return StateEquivalenceChecker.EquivalenceResult.DIFFERENT;
		}

		var i1 = symbols.stream().map(model1::getInterpretation).toList();
		var i2 = symbols.stream().map(model2::getInterpretation).toList();

		return equivalenceChecker.constructMorphism(individuals, i1, s1.objectCode(), i2, s2.objectCode());
	}

	@Override
	public StateCoderAdapter createModelAdapter(Model model) {
		var interpretations = symbols.stream().map(model::getInterpretation).toList();
		var coder = codeCalculatorFactory.create(model, interpretations, individuals);
		return new StateCoderAdapterImpl(this, coder, model);
	}
}
