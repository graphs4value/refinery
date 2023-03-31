package tools.refinery.store.reasoning.representation;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.term.NodeSort;
import tools.refinery.store.query.term.Sort;
import tools.refinery.store.representation.AbstractDomain;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.TruthValueDomain;

import java.util.Arrays;
import java.util.List;

public record PartialRelation(String name, int arity) implements PartialSymbol<TruthValue, Boolean>, Constraint {
	@Override
	public AbstractDomain<TruthValue, Boolean> abstractDomain() {
		return TruthValueDomain.INSTANCE;
	}

	@Override
	public TruthValue defaultValue() {
		return TruthValue.FALSE;
	}

	@Override
	public Boolean defaultConcreteValue() {
		return false;
	}

	@Override
	public List<Sort> getSorts() {
		var sorts = new Sort[arity()];
		Arrays.fill(sorts, NodeSort.INSTANCE);
		return List.of(sorts);
	}

	@Override
	public String toReferenceString() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		// Compare by identity to make hash table lookups more efficient.
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		return "%s/%d".formatted(name, arity);
	}
}
