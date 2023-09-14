/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

import tools.refinery.store.dse.transition.actions.Action;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.dse.transition.callback.*;
import tools.refinery.store.query.dnf.AbstractQueryBuilder;
import tools.refinery.store.query.dnf.Dnf;
import tools.refinery.store.query.term.Variable;

import java.util.List;

public class RuleBuilder extends AbstractQueryBuilder<RuleBuilder> {
	private final String name;
	private List<ActionLiteral> action;

	RuleBuilder(String name) {
		super(Dnf.builder(name == null ? null : name + "#precondition"));
		this.name = name;
	}

	@Override
	protected RuleBuilder self() {
		return this;
	}

	public RuleBuilder action(ActionLiteral... literals) {
		return action(List.of(literals));
	}

	public RuleBuilder action(List<? extends ActionLiteral> literals) {
		if (this.action != null) {
			throw new IllegalStateException("Actions have already been set");
		}
		this.action = List.copyOf(literals);
		return this;
	}

	public RuleBuilder action(Action action) {
		return action(action.getActionLiterals());
	}

	public RuleBuilder action(ActionCallback0 callback) {
		return action(callback.toLiterals());
	}

	public RuleBuilder action(ActionCallback1 callback) {
		return action(callback.toLiterals(Variable.of("v1")));
	}

	public RuleBuilder action(ActionCallback2 callback) {
		return action(callback.toLiterals(Variable.of("v1"), Variable.of("v2")));
	}

	public RuleBuilder action(ActionCallback3 callback) {
		return action(callback.toLiterals(Variable.of("v1"), Variable.of("v2"), Variable.of("v3")));
	}

	public RuleBuilder action(ActionCallback4 callback) {
		return action(callback.toLiterals(Variable.of("v1"), Variable.of("v2"), Variable.of("v3"), Variable.of("v4")));
	}

	public Rule build() {
		var precondition = dnfBuilder.build().asRelation();
		return new Rule(name, precondition, Action.ofPrecondition(precondition, action));
	}
}
