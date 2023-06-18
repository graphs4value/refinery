/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.valuation;

import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.DataVariable;

public record SubstitutedValuation(Valuation originalValuation, Substitution substitution) implements Valuation {
	@Override
	public <T> T getValue(DataVariable<T> variable) {
		return originalValuation.getValue(substitution.getTypeSafeSubstitute(variable));
	}
}
