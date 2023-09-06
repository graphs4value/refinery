/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.actions;

import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.representation.Symbol;

import java.util.List;
import java.util.Objects;

public final class ActionLiterals {
	private ActionLiterals() {
		throw new IllegalArgumentException("This is a static utility class and should not be instantiated directly");
	}

	public static <T> PutActionLiteral<T> put(Symbol<T> symbol, T value, NodeVariable... parameters) {
		return new PutActionLiteral<>(symbol, value, List.of(parameters));
	}

	public static PutActionLiteral<Boolean> add(Symbol<Boolean> symbol, NodeVariable... parameters) {
		if (!Objects.equals(symbol.defaultValue(), false)) {
			throw new IllegalArgumentException("Use put to add a value to symbols other than two-valued logic");
		}
		return put(symbol, true, parameters);
	}

	public static <T> PutActionLiteral<T> remove(Symbol<T> symbol, NodeVariable... parameters) {
		return put(symbol, symbol.defaultValue(), parameters);
	}
}
