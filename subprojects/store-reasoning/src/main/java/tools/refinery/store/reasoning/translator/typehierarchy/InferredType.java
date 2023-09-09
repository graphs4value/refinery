/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public final class InferredType {
	public static final InferredType UNTYPED = new InferredType(Set.of(), Set.of(), null);
	private final Set<PartialRelation> mustTypes;
	private final Set<PartialRelation> mayConcreteTypes;
	private final PartialRelation candidateType;
	private final int hashCode;

	public InferredType(Set<PartialRelation> mustTypes, Set<PartialRelation> mayConcreteTypes,
						PartialRelation candidateType) {
		this.mustTypes = Collections.unmodifiableSet(mustTypes);
		this.mayConcreteTypes = Collections.unmodifiableSet(mayConcreteTypes);
		this.candidateType = candidateType;
		hashCode = Objects.hash(mustTypes, mayConcreteTypes, candidateType);
	}

	public boolean isConsistent() {
		return candidateType != null || mustTypes.isEmpty();
	}

	public boolean isMust(PartialRelation partialRelation) {
		return mustTypes.contains(partialRelation);
	}

	public boolean isMayConcrete(PartialRelation partialRelation) {
		return mayConcreteTypes.contains(partialRelation);
	}


	public Set<PartialRelation> mustTypes() {
		return mustTypes;
	}

	public Set<PartialRelation> mayConcreteTypes() {
		return mayConcreteTypes;
	}

	public PartialRelation candidateType() {
		return candidateType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		InferredType that = (InferredType) o;
		return Objects.equals(mustTypes, that.mustTypes) &&
				Objects.equals(mayConcreteTypes, that.mayConcreteTypes) &&
				Objects.equals(candidateType, that.candidateType);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return "InferredType[" +
				"mustTypes=" + mustTypes + ", " +
				"mayConcreteTypes=" + mayConcreteTypes + ", " +
				"candidateType=" + candidateType + ']';
	}
}
