/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiplicity;

import tools.refinery.logic.term.cardinalityinterval.CardinalityInterval;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;

public record ConstrainedMultiplicity(CardinalityInterval multiplicity, PartialRelation errorSymbol)
		implements Multiplicity {
	public ConstrainedMultiplicity {
		if (multiplicity.isError()) {
			throw new TranslationException(errorSymbol, "Inconsistent multiplicity");
		}
		if (multiplicity.equals(CardinalityIntervals.SET)) {
			throw new TranslationException(errorSymbol, "Expected a constrained cardinality interval");
		}
		if (errorSymbol.arity() != 1) {
			throw new TranslationException(errorSymbol, "Expected error symbol %s to have arity 1, got %d instead"
					.formatted(errorSymbol, errorSymbol.arity()));
		}
	}

	@Override
	public boolean isConstrained() {
		return true;
	}
}
