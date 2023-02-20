package tools.refinery.store.partial.representation;

import tools.refinery.store.partial.literal.Modality;
import tools.refinery.store.partial.literal.PartialRelationLiteral;
import tools.refinery.store.partial.literal.ModalRelationLiteral;
import tools.refinery.store.query.RelationLike;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.representation.AbstractDomain;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.representation.TruthValueDomain;

import java.util.List;

public record PartialRelation(String name, int arity) implements PartialSymbol<TruthValue, Boolean>, RelationLike {
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

	public ModalRelationLiteral call(CallPolarity polarity, Modality modality, List<Variable> arguments) {
		return new ModalRelationLiteral(polarity, modality, this, arguments);
	}

	public PartialRelationLiteral call(CallPolarity polarity, List<Variable> arguments) {
		return new PartialRelationLiteral(polarity, this, arguments);
	}

	public PartialRelationLiteral call(CallPolarity polarity, Variable... arguments) {
		return call(polarity, List.of(arguments));
	}

	public PartialRelationLiteral call(Variable... arguments) {
		return call(CallPolarity.POSITIVE, arguments);
	}

	public PartialRelationLiteral callTransitive(Variable left, Variable right) {
		return call(CallPolarity.TRANSITIVE, List.of(left, right));
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
