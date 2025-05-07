/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.*;

public record TypeInfo(Set<PartialRelation> supertypes, boolean abstractType, boolean decide) {
	public TypeInfo(Collection<PartialRelation> supertypes, boolean abstractType) {
		this(supertypes, abstractType, true);
	}

	public TypeInfo(Collection<PartialRelation> supertypes, boolean abstractType, boolean decide) {
		this(Set.copyOf(supertypes), abstractType, decide);
	}

	public TypeInfo addSupertype(PartialRelation newSupertype) {
		var newSupertypes = new ArrayList<PartialRelation>(supertypes.size() + 1);
		newSupertypes.addAll(supertypes);
		newSupertypes.add(newSupertype);
		return new TypeInfo(newSupertypes, abstractType, decide);
	}
}
