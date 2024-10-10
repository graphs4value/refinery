/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.logic.term.truthvalue.TruthValue;

import java.util.LinkedHashSet;

public record ReferenceInfo(boolean containment, PartialRelation sourceType, Multiplicity multiplicity,
							PartialRelation targetType, PartialRelation opposite, TruthValue defaultValue,
							boolean partial, LinkedHashSet<PartialRelation> supersets) {
	public ReferenceInfo {
		if (containment && partial) {
			throw new IllegalArgumentException("Containment references cannot be partial");
		}
	}

	public static ReferenceInfoBuilder builder() {
		return new ReferenceInfoBuilder();
	}
}
