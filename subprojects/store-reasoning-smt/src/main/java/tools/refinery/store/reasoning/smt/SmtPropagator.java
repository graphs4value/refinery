/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt;

import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.reasoning.smt.internal.BoundSmtPropagator;
import tools.refinery.store.reasoning.smt.internal.PreparedSmtRule;
import tools.refinery.z3.Z3SolverLoader;

import java.util.ArrayList;
import java.util.List;

public class SmtPropagator implements ModelStoreConfiguration {
	private final List<SmtRule> rules = new ArrayList<>();

	public SmtPropagator rule(RelationalQuery precondition, Term<TruthValue> assertedTerm) {
		return rule(new SmtRule(precondition, assertedTerm));
	}

	public SmtPropagator rule(SmtRule rule) {
		rules.add(rule);
		return this;
	}

	public SmtPropagator rules(SmtRule... rules) {
		return rules(List.of(rules));
	}

	public SmtPropagator rules(List<SmtRule> rules) {
		this.rules.addAll(rules);
		return this;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		Z3SolverLoader.loadNativeLibraries();
		var preparedRules = rules.stream().map(PreparedSmtRule::of).toList();
		var queryEngineBuilder = storeBuilder.getAdapter(ModelQueryBuilder.class);
		for (var preparedRule : preparedRules) {
			queryEngineBuilder.queries(preparedRule.partialPrecondition(), preparedRule.candidatePrecondition());
		}
		storeBuilder.getAdapter(PropagationBuilder.class)
				.propagator(model -> new BoundSmtPropagator(this, model, preparedRules));
	}
}
