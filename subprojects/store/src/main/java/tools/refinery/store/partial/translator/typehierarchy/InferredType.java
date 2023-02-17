package tools.refinery.store.partial.translator.typehierarchy;

import tools.refinery.store.partial.representation.PartialRelation;

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
