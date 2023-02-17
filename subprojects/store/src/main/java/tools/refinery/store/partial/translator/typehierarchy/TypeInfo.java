package tools.refinery.store.partial.translator.typehierarchy;

import tools.refinery.store.partial.representation.PartialRelation;

import java.util.*;

public record TypeInfo(Collection<PartialRelation> supertypes, boolean abstractType) {
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final Set<PartialRelation> supertypes = new LinkedHashSet<>();
		private boolean abstractType;

		private Builder() {
		}

		public Builder supertypes(Collection<PartialRelation> supertypes) {
			this.supertypes.addAll(supertypes);
			return this;
		}

		public Builder supertypes(PartialRelation... supertypes) {
			return supertypes(List.of(supertypes));
		}

		public Builder supertype(PartialRelation supertype) {
			supertypes.add(supertype);
			return this;
		}

		public Builder abstractType(boolean abstractType) {
			this.abstractType = abstractType;
			return this;
		}

		public Builder abstractType() {
			return abstractType(true);
		}

		public TypeInfo build() {
			return new TypeInfo(Collections.unmodifiableSet(supertypes), abstractType);
		}
	}
}
