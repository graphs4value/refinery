/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;

public record DirectedCrossReferenceInfo(PartialRelation sourceType, Multiplicity sourceMultiplicity,
										 PartialRelation targetType, Multiplicity targetMultiplicity) {
	public boolean isConstrained() {
		return sourceMultiplicity.isConstrained() || targetMultiplicity.isConstrained();
	}
}
