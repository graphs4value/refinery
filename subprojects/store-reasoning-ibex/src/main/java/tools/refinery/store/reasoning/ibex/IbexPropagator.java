/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.ibex;

import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.ibex.internal.BoundIbexPropagator;
import tools.refinery.store.reasoning.ibex.internal.PreparedIbexRule;
import tools.refinery.store.reasoning.literal.Concreteness;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class IbexPropagator implements ModelStoreConfiguration {
	private final List<IbexRule> rules = new ArrayList<>();

	public IbexPropagator rule(RelationalQuery precondition, Term<TruthValue> assertedTerm) {
		return rule(new IbexRule(precondition, assertedTerm));
	}

	public IbexPropagator rule(IbexRule rule) {
		rules.add(rule);
		return this;
	}

	public IbexPropagator rules(IbexRule... rules) {
		return rules(List.of(rules));
	}

	public IbexPropagator rules(Collection<IbexRule> rules) {
		this.rules.addAll(rules);
		return this;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		// PreparedIbexRule.of() creates the Ibex solver and calls build() — native library must be loaded.
		var preparedRules = rules.stream().map(PreparedIbexRule::of).toList();

		var queryEngineBuilder = storeBuilder.getAdapter(ModelQueryBuilder.class);
		for (var preparedRule : preparedRules) {
			queryEngineBuilder.queries(preparedRule.partialPrecondition());
		}

		storeBuilder.getAdapter(PropagationBuilder.class)
				.propagator(model -> new BoundIbexPropagator(this, model, preparedRules));

		storeBuilder.getAdapter(ReasoningBuilder.class)
				.requiredInterpretations(Set.of(Concreteness.PARTIAL, Concreteness.CANDIDATE));
	}
}
