package tools.refinery.store.reasoning.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.literal.LiteralReduction;
import tools.refinery.store.query.term.Sort;

import java.util.List;

public record ModalConstraint(Modality modality, Constraint constraint) implements Constraint {
	private static final String FORMAT = "%s %s";

	@Override
	public String name() {
		return FORMAT.formatted(modality, constraint.name());
	}

	@Override
	public List<Sort> getSorts() {
		return constraint.getSorts();
	}

	@Override
	public LiteralReduction getReduction() {
		return constraint.getReduction();
	}

	@Override
	public boolean equals(LiteralEqualityHelper helper, Constraint other) {
		if (getClass() != other.getClass()) {
			return false;
		}
		var otherModalConstraint = (ModalConstraint) other;
		return modality == otherModalConstraint.modality && constraint.equals(helper, otherModalConstraint.constraint);
	}

	@Override
	public String toReferenceString() {
		return FORMAT.formatted(modality, constraint.toReferenceString());
	}

	@Override
	public String toString() {
		return FORMAT.formatted(modality, constraint);
	}
}
