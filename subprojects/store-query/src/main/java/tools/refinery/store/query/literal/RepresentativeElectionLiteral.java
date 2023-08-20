/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.equality.LiteralHashCodeHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.NodeVariable;
import tools.refinery.store.query.term.ParameterDirection;
import tools.refinery.store.query.term.Variable;

import java.util.List;
import java.util.Set;

// {@link Object#equals(Object)} is implemented by {@link AbstractLiteral}.
@SuppressWarnings("squid:S2160")
public class RepresentativeElectionLiteral extends AbstractCallLiteral {
	private final Connectivity connectivity;

	public RepresentativeElectionLiteral(Connectivity connectivity, Constraint target, NodeVariable specific,
										 NodeVariable representative) {
		this(connectivity, target, List.of(specific, representative));
	}

	private RepresentativeElectionLiteral(Connectivity connectivity, Constraint target, List<Variable> arguments) {
		super(target, arguments);
		this.connectivity = connectivity;
		var parameters = target.getParameters();
		int arity = target.arity();
		if (arity != 2) {
			throw new InvalidQueryException("SCCs can only take binary relations");
		}
		if (parameters.get(0).isDataVariable() || parameters.get(1).isDataVariable()) {
			throw new InvalidQueryException("SCCs can only be computed over nodes");
		}
		if (parameters.get(0).getDirection() != ParameterDirection.OUT ||
				parameters.get(1).getDirection() != ParameterDirection.OUT) {
			throw new InvalidQueryException("SCCs cannot take input parameters");
		}
	}

	public Connectivity getConnectivity() {
		return connectivity;
	}

	@Override
	protected Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new RepresentativeElectionLiteral(connectivity, getTarget(), substitutedArguments);
	}

	@Override
	public Set<Variable> getOutputVariables() {
		return getArgumentsOfDirection(ParameterDirection.OUT);
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		return Set.of();
	}

	@Override
	public Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		return Set.of();
	}

	@Override
	public Literal reduce() {
		var reduction = getTarget().getReduction();
		return switch (reduction) {
			case ALWAYS_FALSE -> BooleanLiteral.FALSE;
			case ALWAYS_TRUE -> throw new InvalidQueryException(
					"Trying to elect representatives over an infinite set");
			case NOT_REDUCIBLE -> this;
		};
	}

	@Override
	public AbstractCallLiteral withArguments(Constraint newTarget, List<Variable> newArguments) {
		return new RepresentativeElectionLiteral(connectivity, newTarget, newArguments);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherRepresentativeElectionLiteral = (RepresentativeElectionLiteral) other;
		return connectivity.equals(otherRepresentativeElectionLiteral.connectivity);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return super.hashCodeWithSubstitution(helper) * 31 + connectivity.hashCode();
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append("@Representative(\"");
		builder.append(connectivity);
		builder.append("\") ");
		builder.append(getTarget().toReferenceString());
		builder.append("(");
		var argumentIterator = getArguments().iterator();
		if (argumentIterator.hasNext()) {
			builder.append(argumentIterator.next());
			while (argumentIterator.hasNext()) {
				builder.append(", ").append(argumentIterator.next());
			}
		}
		builder.append(")");
		return builder.toString();
	}
}
