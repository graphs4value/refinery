/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.store.representation.TruthValue;

public record UndirectedCrossReferenceInfo(PartialRelation type, Multiplicity multiplicity, TruthValue defaultValue) {
	public boolean isConstrained() {
		return multiplicity.isConstrained();
	}
}
