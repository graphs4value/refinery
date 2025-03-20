/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.InvalidQueryException;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.substitution.Substitution;

import java.util.List;
import java.util.Objects;
import java.util.Set;

// {@link Object#equals(Object)} is implemented by {@link AbstractTerm}.
@SuppressWarnings("squid:S2160")
public class AggregationTerm<R, T> extends AbstractCallTerm<R> {
	private final Aggregator<R, T> aggregator;
	private final DataVariable<T> inputVariable;

	public AggregationTerm(Aggregator<R, T> aggregator, DataVariable<T> inputVariable, Constraint target,
						   List<Variable> arguments) {
		super(aggregator.getResultType(), target, arguments);
		if (!inputVariable.getType().equals(aggregator.getInputType())) {
			throw new InvalidQueryException("Input variable %s must of type %s, got %s instead".formatted(
					inputVariable, aggregator.getInputType().getName(), inputVariable.getType().getName()));
		}
		if (!getArgumentsOfDirection(ParameterDirection.OUT).contains(inputVariable)) {
			throw new InvalidQueryException("Input variable %s must be bound with direction %s in the argument list"
					.formatted(inputVariable, ParameterDirection.OUT));
		}
		this.aggregator = aggregator;
		this.inputVariable = inputVariable;
	}

	public DataVariable<T> getInputVariable() {
		return inputVariable;
	}

	public Aggregator<R, T> getAggregator() {
		return aggregator;
	}

	@Override
	protected Term<R> doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new AggregationTerm<>(aggregator, substitution.getTypeSafeSubstitute(inputVariable), getTarget(),
				substitutedArguments);
	}

	@Override
	public Term<R> withArguments(Constraint newTarget, List<Variable> newArguments) {
		return new AggregationTerm<>(aggregator, inputVariable, newTarget, newArguments);
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		if (positiveVariablesInClause.contains(inputVariable)) {
			throw new InvalidQueryException("Aggregation variable %s must not be bound".formatted(inputVariable));
		}
		return super.getInputVariables(positiveVariablesInClause);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherAggregationTerm = (AggregationTerm<?, ?>) other;
		return Objects.equals(aggregator, otherAggregationTerm.aggregator) &&
				helper.variableEqual(inputVariable, otherAggregationTerm.inputVariable);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), aggregator,
				helper.getVariableHashCode(inputVariable));
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append(getTarget().toReferenceString());
		builder.append("(");
		var argumentIterator = getArguments().iterator();
		if (argumentIterator.hasNext()) {
			var argument = argumentIterator.next();
			if (inputVariable.equals(argument)) {
				builder.append("@Aggregate(\"").append(aggregator).append("\") ");
			}
			builder.append(argument);
			while (argumentIterator.hasNext()) {
				builder.append(", ");
				argument = argumentIterator.next();
				if (inputVariable.equals(argument)) {
					builder.append("@Aggregate(\"").append(aggregator).append("\") ");
				}
				builder.append(argument);
			}
		}
		builder.append(")");
		return builder.toString();
	}
}
