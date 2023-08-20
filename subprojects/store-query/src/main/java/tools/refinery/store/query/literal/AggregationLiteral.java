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
import tools.refinery.store.query.term.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;

// {@link Object#equals(Object)} is implemented by {@link AbstractLiteral}.
@SuppressWarnings("squid:S2160")
public class AggregationLiteral<R, T> extends AbstractCallLiteral {
	private final DataVariable<R> resultVariable;
	private final DataVariable<T> inputVariable;
	private final Aggregator<R, T> aggregator;

	public AggregationLiteral(DataVariable<R> resultVariable, Aggregator<R, T> aggregator,
							  DataVariable<T> inputVariable, Constraint target, List<Variable> arguments) {
		super(target, arguments);
		if (!inputVariable.getType().equals(aggregator.getInputType())) {
			throw new InvalidQueryException("Input variable %s must of type %s, got %s instead".formatted(
					inputVariable, aggregator.getInputType().getName(), inputVariable.getType().getName()));
		}
		if (!getArgumentsOfDirection(ParameterDirection.OUT).contains(inputVariable)) {
			throw new InvalidQueryException("Input variable %s must be bound with direction %s in the argument list"
					.formatted(inputVariable, ParameterDirection.OUT));
		}
		if (!resultVariable.getType().equals(aggregator.getResultType())) {
			throw new InvalidQueryException("Result variable %s must of type %s, got %s instead".formatted(
					resultVariable, aggregator.getResultType().getName(), resultVariable.getType().getName()));
		}
		if (arguments.contains(resultVariable)) {
			throw new InvalidQueryException("Result variable %s must not appear in the argument list".formatted(
					resultVariable));
		}
		this.resultVariable = resultVariable;
		this.inputVariable = inputVariable;
		this.aggregator = aggregator;
	}

	public DataVariable<R> getResultVariable() {
		return resultVariable;
	}

	public DataVariable<T> getInputVariable() {
		return inputVariable;
	}

	public Aggregator<R, T> getAggregator() {
		return aggregator;
	}

	@Override
	public Set<Variable> getOutputVariables() {
		return Set.of(resultVariable);
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		if (positiveVariablesInClause.contains(inputVariable)) {
			throw new InvalidQueryException("Aggregation variable %s must not be bound".formatted(inputVariable));
		}
		return super.getInputVariables(positiveVariablesInClause);
	}

	@Override
	public Literal reduce() {
		var reduction = getTarget().getReduction();
		return switch (reduction) {
			case ALWAYS_FALSE -> {
				var emptyValue = aggregator.getEmptyResult();
				yield emptyValue == null ? BooleanLiteral.FALSE :
						resultVariable.assign(new ConstantTerm<>(resultVariable.getType(), emptyValue));
			}
			case ALWAYS_TRUE -> throw new InvalidQueryException("Trying to aggregate over an infinite set");
			case NOT_REDUCIBLE -> this;
		};
	}

	@Override
	protected Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new AggregationLiteral<>(substitution.getTypeSafeSubstitute(resultVariable), aggregator,
				substitution.getTypeSafeSubstitute(inputVariable), getTarget(), substitutedArguments);
	}

	@Override
	public AbstractCallLiteral withArguments(Constraint newTarget, List<Variable> newArguments) {
		return new AggregationLiteral<>(resultVariable, aggregator, inputVariable, newTarget, newArguments);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherAggregationLiteral = (AggregationLiteral<?, ?>) other;
		return helper.variableEqual(resultVariable, otherAggregationLiteral.resultVariable) &&
				aggregator.equals(otherAggregationLiteral.aggregator) &&
				helper.variableEqual(inputVariable, otherAggregationLiteral.inputVariable);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), helper.getVariableHashCode(resultVariable),
				helper.getVariableHashCode(inputVariable), aggregator);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append(resultVariable);
		builder.append(" is ");
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
				builder.append(", ").append(argumentIterator.next());
			}
		}
		builder.append(")");
		return builder.toString();
	}
}
