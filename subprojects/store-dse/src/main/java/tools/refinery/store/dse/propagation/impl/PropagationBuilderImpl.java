/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation.impl;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.propagation.PropagationStoreAdapter;
import tools.refinery.store.dse.propagation.Propagator;
import tools.refinery.store.dse.propagation.impl.rule.RuleBasedPropagator;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;

import java.util.*;

public class PropagationBuilderImpl extends AbstractModelAdapterBuilder<PropagationStoreAdapter>
		implements PropagationBuilder {
	private final Set<Rule> propagationRules = new LinkedHashSet<>();
	private final Deque<Propagator> propagators = new ArrayDeque<>();

	@Override
	public PropagationBuilder rule(Rule propagationRule) {
		checkNotConfigured();
		propagationRules.add(propagationRule);
		return this;
	}

	@Override
	public PropagationBuilder propagator(Propagator propagator) {
		checkNotConfigured();
		propagators.addFirst(propagator);
		return this;
	}

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		super.doConfigure(storeBuilder);
		if (!propagationRules.isEmpty()) {
			propagators.addFirst(new RuleBasedPropagator(List.copyOf(propagationRules)));
		}
		for (var propagator : propagators) {
			propagator.configure(storeBuilder);
		}
	}

	@Override
	protected PropagationStoreAdapter doBuild(ModelStore store) {
		return new PropagationStoreAdapterImpl(store, List.copyOf(propagators));
	}
}
