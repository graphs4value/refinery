/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.NodeSort;
import tools.refinery.store.query.term.Variable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CallLiteral extends AbstractCallLiteral implements CanNegate<CallLiteral> {
	private final CallPolarity polarity;

	public CallLiteral(CallPolarity polarity, Constraint target, List<Variable> arguments) {
		super(target, arguments);
		if (polarity.isTransitive()) {
			if (target.arity() != 2) {
				throw new IllegalArgumentException("Transitive closures can only take binary relations");
			}
			var sorts = target.getSorts();
			if (!sorts.get(0).equals(NodeSort.INSTANCE) || !sorts.get(1).equals(NodeSort.INSTANCE)) {
				throw new IllegalArgumentException("Transitive closures can only be computed over nodes");
			}
		}
		this.polarity = polarity;
	}

	public CallPolarity getPolarity() {
		return polarity;
	}

	@Override
	public Set<Variable> getBoundVariables() {
		return polarity.isPositive() ? Set.copyOf(getArguments()) : Set.of();
	}

	@Override
	protected Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new CallLiteral(polarity, getTarget(), substitutedArguments);
	}

	@Override
	public LiteralReduction getReduction() {
		var reduction = getTarget().getReduction();
		return polarity.isPositive() ? reduction : reduction.negate();
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherCallLiteral = (CallLiteral) other;
		return polarity.equals(otherCallLiteral.polarity);
	}

	@Override
	public CallLiteral negate() {
		return new CallLiteral(polarity.negate(), getTarget(), getArguments());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		CallLiteral that = (CallLiteral) o;
		return polarity == that.polarity;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), polarity);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		if (!polarity.isPositive()) {
			builder.append("!(");
		}
		builder.append(getTarget().toReferenceString());
		if (polarity.isTransitive()) {
			builder.append("+");
		}
		builder.append("(");
		var argumentIterator = getArguments().iterator();
		if (argumentIterator.hasNext()) {
			builder.append(argumentIterator.next());
			while (argumentIterator.hasNext()) {
				builder.append(", ").append(argumentIterator.next());
			}
		}
		builder.append(")");
		if (!polarity.isPositive()) {
			builder.append(")");
		}
		return builder.toString();
	}
}
