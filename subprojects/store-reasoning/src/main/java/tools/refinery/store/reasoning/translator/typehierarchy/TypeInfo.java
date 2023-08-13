/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.*;

public record TypeInfo(Set<PartialRelation> supertypes, boolean abstractType) {
	public TypeInfo(Collection<PartialRelation> supertypes, boolean abstractType) {
		this(Set.copyOf(supertypes), abstractType);
	}

	public TypeInfo addSupertype(PartialRelation newSupertype) {
		var newSupertypes = new ArrayList<PartialRelation>(supertypes.size() + 1);
		newSupertypes.addAll(supertypes);
		newSupertypes.add(newSupertype);
		return new TypeInfo(newSupertypes, abstractType);
	}
}
