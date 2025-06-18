/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.valuation;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.DataVariable;
import tools.refinery.logic.term.NodeVariable;

import java.util.Map;

public interface Valuation {
	<T> T getValue(DataVariable<T> variable);

	Integer getNodeId(NodeVariable nodeVariable);

	default Valuation substitute(@Nullable Substitution substitution) {
		if (substitution == null) {
			return this;
		}
		return new SubstitutedValuation(this, substitution);
	}

	static ValuationBuilder builder() {
		return new ValuationBuilder();
	}

	static Valuation empty() {
		return new MapBasedValuation(Map.of());
	}
}
