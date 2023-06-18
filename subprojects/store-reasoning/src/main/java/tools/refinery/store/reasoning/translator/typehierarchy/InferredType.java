/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.Collections;
import java.util.Set;

record InferredType(Set<PartialRelation> mustTypes, Set<PartialRelation> mayConcreteTypes,
					PartialRelation currentType) {
	public static final InferredType UNTYPED = new InferredType(Set.of(), Set.of(), null);

	public InferredType(Set<PartialRelation> mustTypes, Set<PartialRelation> mayConcreteTypes,
						PartialRelation currentType) {
		this.mustTypes = Collections.unmodifiableSet(mustTypes);
		this.mayConcreteTypes = Collections.unmodifiableSet(mayConcreteTypes);
		this.currentType = currentType;
	}

	public boolean isConsistent() {
		return currentType != null || mustTypes.isEmpty();
	}

	public boolean isMust(PartialRelation partialRelation) {
		return mustTypes.contains(partialRelation);
	}

	public boolean isMayConcrete(PartialRelation partialRelation) {
		return mayConcreteTypes.contains(partialRelation);
	}
}
