/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.valuation;

import tools.refinery.store.query.term.AnyDataVariable;
import tools.refinery.store.query.term.DataVariable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ValuationBuilder {
	private final Map<AnyDataVariable, Object> values = new HashMap<>();

	ValuationBuilder() {
	}

	public <T> ValuationBuilder put(DataVariable<T> variable, T value) {
		return putChecked(variable, value);
	}

	public ValuationBuilder putChecked(AnyDataVariable variable, Object value) {
		if (value != null && !variable.getType().isInstance(value)) {
			throw new IllegalArgumentException("Value %s is not an instance of %s"
					.formatted(value, variable.getType().getName()));
		}
		if (values.containsKey(variable)) {
			throw new IllegalArgumentException("Already has value for variable %s".formatted(variable));
		}
		values.put(variable, value);
		return this;
	}

	public Valuation build() {
		return new MapBasedValuation(Collections.unmodifiableMap(values));
	}
}
