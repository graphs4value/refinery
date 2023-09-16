/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal.pquery;

import tools.refinery.interpreter.matchers.psystem.IValueProvider;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.valuation.Valuation;

public record ValueProviderBasedValuation(IValueProvider valueProvider) implements Valuation {
	@Override
	public <T> T getValue(DataVariable<T> variable) {
		@SuppressWarnings("unchecked")
		var value = (T) valueProvider.getValue(variable.getUniqueName());
		return value;
	}
}
