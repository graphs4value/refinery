/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition;

import tools.refinery.store.dse.transition.actions.Action;
import tools.refinery.store.dse.transition.actions.BoundAction;
import tools.refinery.store.dse.transition.callback.*;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.dnf.RelationalQuery;

public class Rule {
	private final String name;
	private final RelationalQuery precondition;
	private final Action action;

	public Rule(String name, RelationalQuery precondition, Action action) {
		if (precondition.arity() != action.getArity()) {
			throw new IllegalArgumentException("Expected an action clause with %d parameters, got %d instead"
					.formatted(precondition.arity(), action.getArity()));
		}
		this.name = name;
		this.precondition = precondition;
		this.action = action;
	}

	public String getName() {
		return name;
	}

	public RelationalQuery getPrecondition() {
		return precondition;
	}

	public BoundAction createAction(Model model) {
		return action.bindToModel(model);
	}

	public static RuleBuilder builder(String name) {
		return new RuleBuilder(name);
	}

	public static RuleBuilder builder() {
		return builder(null);
	}

	public static Rule of(String name, RuleCallback0 callback) {
		var builder = builder(name);
		callback.accept(builder);
		return builder.build();
	}

	public static Rule of(RuleCallback0 callback) {
		return of(null, callback);
	}

	public static Rule of(String name, RuleCallback1 callback) {
		var builder = builder(name);
		callback.accept(builder, builder.parameter("p1"));
		return builder.build();
	}

	public static Rule of(RuleCallback1 callback) {
		return of(null, callback);
	}

	public static Rule of(String name, RuleCallback2 callback) {
		var builder = builder(name);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"));
		return builder.build();
	}

	public static Rule of(RuleCallback2 callback) {
		return of(null, callback);
	}

	public static Rule of(String name, RuleCallback3 callback) {
		var builder = builder(name);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"), builder.parameter("p3"));
		return builder.build();
	}

	public static Rule of(RuleCallback3 callback) {
		return of(null, callback);
	}

	public static Rule of(String name, RuleCallback4 callback) {
		var builder = builder(name);
		callback.accept(builder, builder.parameter("p1"), builder.parameter("p2"), builder.parameter("p3"),
				builder.parameter("p4"));
		return builder.build();
	}

	public static Rule of(RuleCallback4 callback) {
		return of(null, callback);
	}
}
