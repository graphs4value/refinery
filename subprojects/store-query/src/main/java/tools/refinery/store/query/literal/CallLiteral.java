/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.equality.LiteralHashCodeHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;

import java.util.*;

// {@link Object#equals(Object)} is implemented by {@link AbstractLiteral}.
@SuppressWarnings("squid:S2160")
public final class CallLiteral extends AbstractCallLiteral implements CanNegate<CallLiteral> {
	private final CallPolarity polarity;

	public CallLiteral(CallPolarity polarity, Constraint target, List<Variable> arguments) {
		super(target, arguments);
		var parameters = target.getParameters();
		int arity = target.arity();
		if (polarity.isTransitive()) {
			if (arity != 2) {
				throw new InvalidQueryException("Transitive closures can only take binary relations");
			}
			if (parameters.get(0).isDataVariable() || parameters.get(1).isDataVariable()) {
				throw new InvalidQueryException("Transitive closures can only be computed over nodes");
			}
			if (parameters.get(0).getDirection() != ParameterDirection.OUT ||
					parameters.get(1).getDirection() != ParameterDirection.OUT) {
				throw new InvalidQueryException("Transitive closures cannot take input parameters");
			}
		}
		this.polarity = polarity;
	}

	public CallPolarity getPolarity() {
		return polarity;
	}

	@Override
	protected Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new CallLiteral(polarity, getTarget(), substitutedArguments);
	}

	@Override
	public Set<Variable> getOutputVariables() {
		if (polarity.isPositive()) {
			return getArgumentsOfDirection(ParameterDirection.OUT);
		}
		return Set.of();
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		if (polarity.isPositive()) {
			return getArgumentsOfDirection(ParameterDirection.IN);
		}
		return super.getInputVariables(positiveVariablesInClause);
	}

	@Override
	public Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		if (polarity.isPositive()) {
			return Set.of();
		}
		return super.getPrivateVariables(positiveVariablesInClause);
	}

	@Override
	public Literal reduce() {
		var reduction = getTarget().getReduction();
		var negatedReduction = polarity.isPositive() ? reduction : reduction.negate();
		return switch (negatedReduction) {
			case ALWAYS_TRUE -> BooleanLiteral.TRUE;
			case ALWAYS_FALSE -> BooleanLiteral.FALSE;
			default -> this;
		};
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
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), polarity);
	}

	@Override
	public CallLiteral negate() {
		return new CallLiteral(polarity.negate(), getTarget(), getArguments());
	}

	@Override
	public AbstractCallLiteral withArguments(Constraint newTarget, List<Variable> newArguments) {
		return new CallLiteral(polarity, newTarget, newArguments);
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
