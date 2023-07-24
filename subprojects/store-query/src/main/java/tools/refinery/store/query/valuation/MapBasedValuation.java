/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.valuation;

import tools.refinery.store.query.term.AnyDataVariable;
import tools.refinery.store.query.term.DataVariable;

import java.util.Map;

record MapBasedValuation(Map<AnyDataVariable, Object> values) implements Valuation {
	@Override
	public <T> T getValue(DataVariable<T> variable) {
		if (!values.containsKey(variable)) {
			throw new IllegalArgumentException("No value for variable %s".formatted(variable));
		}
		var value = values.get(variable);
		return variable.getType().cast(value);
	}
}
