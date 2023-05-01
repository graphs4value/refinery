/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Variable;

import java.util.List;
import java.util.Objects;

public final class CallLiteral extends AbstractCallLiteral implements CanNegate<CallLiteral> {
	private final CallPolarity polarity;
	private final VariableBinder variableBinder;

	public CallLiteral(CallPolarity polarity, Constraint target, List<Variable> arguments) {
		super(target, arguments);
		var parameters = target.getParameters();
		int arity = target.arity();
		if (polarity.isTransitive()) {
			if (arity != 2) {
				throw new IllegalArgumentException("Transitive closures can only take binary relations");
			}
			if (parameters.get(0).isDataVariable() || parameters.get(1).isDataVariable()) {
				throw new IllegalArgumentException("Transitive closures can only be computed over nodes");
			}
		}
		this.polarity = polarity;
		variableBinder = VariableBinder.builder()
				.parameterList(polarity.isPositive(), parameters, arguments)
				.build();
	}

	public CallPolarity getPolarity() {
		return polarity;
	}

	@Override
	public VariableBinder getVariableBinder() {
		return variableBinder;
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
