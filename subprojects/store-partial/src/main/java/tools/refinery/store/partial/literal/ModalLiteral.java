package tools.refinery.store.partial.literal;

import tools.refinery.store.query.RelationLike;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.literal.CallLiteral;
import tools.refinery.store.query.literal.CallPolarity;
import tools.refinery.store.query.literal.Literal;

import java.util.List;
import java.util.Objects;

public abstract class ModalLiteral<T extends RelationLike> extends CallLiteral<T> {
	private final Modality modality;

	protected ModalLiteral(CallPolarity polarity, Modality modality, T target, List<Variable> arguments) {
		super(polarity, target, arguments);
		this.modality = modality;
	}

	protected ModalLiteral(Modality modality, CallLiteral<? extends T> baseLiteral) {
		this(baseLiteral.getPolarity(), commute(modality, baseLiteral.getPolarity()), baseLiteral.getTarget(),
				baseLiteral.getArguments());
	}

	public Modality getModality() {
		return modality;
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		// If {@link CallLiteral#equalsWithSubstitution(LiteralEqualityHelper, Literal)} has returned {@code true},
		// we must have the same dynamic type as {@code other}.
		var otherModalLiteral = (ModalLiteral<?>) other;
		return modality == otherModalLiteral.modality;
	}

	@Override
	protected String targetToString() {
		return "%s %s".formatted(modality, super.targetToString());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		ModalLiteral<?> that = (ModalLiteral<?>) o;
		return modality == that.modality;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), modality);
	}

	private static Modality commute(Modality modality, CallPolarity polarity) {
		return polarity.isPositive() ? modality : modality.negate();
	}
}
