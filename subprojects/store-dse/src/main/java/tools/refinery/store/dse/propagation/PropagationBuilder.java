/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.propagation;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStore;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnusedReturnValue")
public interface PropagationBuilder extends ModelAdapterBuilder {
	PropagationBuilder rule(Rule propagationRule);

	default PropagationBuilder rules(Rule... propagationRules) {
		return rules(List.of(propagationRules));
	}

	default PropagationBuilder rules(Collection<Rule> propagationRules) {
		propagationRules.forEach(this::rule);
		return this;
	}

	PropagationBuilder propagator(Propagator propagator);

	@Override
	PropagationStoreAdapter build(ModelStore store);
}
