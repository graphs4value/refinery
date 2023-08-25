/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.internal.action;

import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple0;

import java.util.Arrays;

public class InsertAction<T> implements AtomicAction {

	private final Interpretation<T> interpretation;
	private final T value;
	private final int arity;
	private final ActionVariable[] variables;

	public InsertAction(Interpretation<T> interpretation, T value, ActionVariable... variables) {
		this.interpretation = interpretation;
		this.value = value;
		this.variables = variables;
		this.arity = interpretation.getSymbol().arity();
		if (variables.length != arity) {
			throw new IllegalArgumentException("Expected " + arity + " variables, but got " + variables.length);
		}
	}

	@Override
	public void fire(Tuple activation) {
		Tuple tuple;
		if (arity == 0) {
			tuple = Tuple0.INSTANCE;
		}
		else if (arity == 1) {
			tuple = variables[0].getValue();
		}
		else if (arity == 2) {
			tuple = Tuple.of(variables[0].getValue().get(0), variables[1].getValue().get(0));
		}
		else if (arity == 3) {
			tuple = Tuple.of(variables[0].getValue().get(0), variables[1].getValue().get(0), variables[2].getValue().get(0));
		}
		else {
			tuple = Tuple.of(Arrays.stream(variables).map(variable -> variable.getValue().get(0))
					.mapToInt(Integer::intValue).toArray());
		}
		interpretation.put(tuple, value);
	}

	public void put(Tuple tuple) {
		interpretation.put(tuple, value);
	}

	@Override
	public InsertAction<T> prepare(Model model) {
		return this;
	}

	public ActionVariable[] getVariables() {
		return variables;
	}

	@Override
	public boolean equalsWithSubstitution(AtomicAction other) {
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		var otherAction = (InsertAction<?>) other;
		if (variables.length != otherAction.variables.length) {
			return false;
		}
		if (!interpretation.equals(otherAction.interpretation)) {
			return false;
		}
		if (value == null) {
			if (otherAction.value != null) {
				return false;
			}
		}
        else if (!value.equals(otherAction.value)) {
			return false;
		}
		for (var i = 0; i < variables.length; i++) {
			if (!variables[i].equalsWithSubstitution(otherAction.variables[i])) {
				return false;
			}
		}
		return true;
	}
}
