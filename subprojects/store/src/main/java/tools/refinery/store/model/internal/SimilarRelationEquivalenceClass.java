package tools.refinery.store.model.internal;

import tools.refinery.store.map.ContinousHashProvider;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

public class SimilarRelationEquivalenceClass {
	final ContinousHashProvider<Tuple> hashProvider;
	final Object defaultValue;
	final int arity;

	public SimilarRelationEquivalenceClass(Relation<?> representation) {
		this.hashProvider = representation.getHashProvider();
		this.defaultValue = representation.getDefaultValue();
		this.arity = representation.getArity();
	}

	@Override
	public int hashCode() {
		return Objects.hash(arity, defaultValue, hashProvider);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof SimilarRelationEquivalenceClass other))
			return false;
		return arity == other.arity && Objects.equals(defaultValue, other.defaultValue)
				&& Objects.equals(hashProvider, other.hashProvider);
	}

}
