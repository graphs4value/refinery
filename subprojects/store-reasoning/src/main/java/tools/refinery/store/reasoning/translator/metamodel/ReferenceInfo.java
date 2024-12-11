/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.ConcretizationSettings;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;

import java.util.Set;

public record ReferenceInfo(boolean containment, PartialRelation sourceType, Multiplicity multiplicity,
							PartialRelation targetType, PartialRelation opposite, TruthValue defaultValue,
							ConcretizationSettings concretizationSettings, Set<PartialRelation> supersets) {
	public ReferenceInfo {
		if (containment && !concretizationSettings.concretize()) {
			throw new IllegalArgumentException("Containment references must be concretized");
		}
	}

	public static ReferenceInfoBuilder builder() {
		return new ReferenceInfoBuilder();
	}
}
