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
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.smt.internal.BoundSmtPropagator;
import tools.refinery.store.reasoning.smt.internal.PreparedSmtRule;
import tools.refinery.store.reasoning.theory.TheoryRule;
import tools.refinery.z3.Z3SolverLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class SmtPropagator implements ModelStoreConfiguration {
	private final List<TheoryRule> rules = new ArrayList<>();
	private int timeout;
	private int rlimit;

	public SmtPropagator rule(RelationalQuery precondition, Term<TruthValue> assertedTerm) {
		return rule(new TheoryRule(precondition, assertedTerm));
	}

	public SmtPropagator rule(TheoryRule rule) {
		rules.add(rule);
		return this;
	}

	public SmtPropagator rules(TheoryRule... rules) {
		return rules(List.of(rules));
	}

	public SmtPropagator rules(Collection<TheoryRule> rules) {
		this.rules.addAll(rules);
		return this;
	}

	public SmtPropagator timeout(int timeout) {
		this.timeout = timeout;
		return this;
	}

	public SmtPropagator rlimit(int rlimit) {
		this.rlimit = rlimit;
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
				.propagator(model -> new BoundSmtPropagator(this, model, preparedRules, timeout, rlimit));
		// SMT rules rely on `PARTIAL` interpretations for attributes.
		storeBuilder.getAdapter(ReasoningBuilder.class)
				.requiredInterpretations(Set.of(Concreteness.PARTIAL, Concreteness.CANDIDATE));
	}
}
