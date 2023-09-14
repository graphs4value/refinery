/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiplicity;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.CardinalityIntervals;
import tools.refinery.store.representation.cardinality.NonEmptyCardinalityInterval;

public record ConstrainedMultiplicity(NonEmptyCardinalityInterval multiplicity, PartialRelation errorSymbol)
		implements Multiplicity {
	public ConstrainedMultiplicity {
		if (multiplicity.equals(CardinalityIntervals.SET)) {
			throw new TranslationException(errorSymbol, "Expected a constrained cardinality interval");
		}
		if (errorSymbol.arity() != 1) {
			throw new TranslationException(errorSymbol, "Expected error symbol %s to have arity 1, got %d instead"
					.formatted(errorSymbol, errorSymbol.arity()));
		}
	}

	public static ConstrainedMultiplicity of(CardinalityInterval multiplicity, PartialRelation errorSymbol) {
		if (!(multiplicity instanceof NonEmptyCardinalityInterval nonEmptyCardinalityInterval)) {
			throw new TranslationException(errorSymbol, "Inconsistent multiplicity");
		}
		return new ConstrainedMultiplicity(nonEmptyCardinalityInterval, errorSymbol);
	}

	@Override
	public boolean isConstrained() {
		return true;
	}
}
