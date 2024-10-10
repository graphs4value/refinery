/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.logic.term.truthvalue.TruthValue;

import java.util.LinkedHashSet;

public record DirectedCrossReferenceInfo(PartialRelation sourceType, Multiplicity sourceMultiplicity,
										 PartialRelation targetType, Multiplicity targetMultiplicity,
										 TruthValue defaultValue, boolean partial,
										 LinkedHashSet<PartialRelation> supersets, LinkedHashSet<PartialRelation> oppositeSupersets) {
	public boolean isConstrained() {
		return sourceMultiplicity.isConstrained() || targetMultiplicity.isConstrained();
	}
}
