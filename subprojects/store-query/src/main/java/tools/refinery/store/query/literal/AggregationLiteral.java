/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.Aggregator;
import tools.refinery.store.query.term.ConstantTerm;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.Variable;

import java.util.List;
import java.util.Objects;

public class AggregationLiteral<R, T> extends AbstractCallLiteral {
	private final DataVariable<R> resultVariable;
	private final DataVariable<T> inputVariable;
	private final Aggregator<R, T> aggregator;
	private final VariableBindingSite variableBindingSite;

	public AggregationLiteral(DataVariable<R> resultVariable, Aggregator<R, T> aggregator,
							  DataVariable<T> inputVariable, Constraint target, List<Variable> arguments) {
		super(target, arguments);
		if (!inputVariable.getType().equals(aggregator.getInputType())) {
			throw new IllegalArgumentException("Input variable %s must of type %s, got %s instead".formatted(
					inputVariable, aggregator.getInputType().getName(), inputVariable.getType().getName()));
		}
		if (!resultVariable.getType().equals(aggregator.getResultType())) {
			throw new IllegalArgumentException("Result variable %s must of type %s, got %s instead".formatted(
					resultVariable, aggregator.getResultType().getName(), resultVariable.getType().getName()));
		}
		if (arguments.contains(resultVariable)) {
			throw new IllegalArgumentException("Result variable %s must not appear in the argument list".formatted(
					resultVariable));
		}
		this.resultVariable = resultVariable;
		this.inputVariable = inputVariable;
		this.aggregator = aggregator;
		variableBindingSite = VariableBindingSite.builder()
				.variable(resultVariable, VariableDirection.OUT)
				.parameterList(false, target.getParameters(), arguments)
				.build();
		if (variableBindingSite.getDirection(inputVariable) != VariableDirection.CLOSURE) {
			throw new IllegalArgumentException(("Input variable %s must appear in the argument list as an output " +
					"variable and should not be bound anywhere else").formatted(inputVariable));
		}
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
	public VariableBindingSite getVariableBindingSite() {
		return variableBindingSite;
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
			case ALWAYS_TRUE -> throw new IllegalArgumentException("Trying to aggregate over an infinite set");
			case NOT_REDUCIBLE -> this;
		};
	}

	@Override
	protected Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new AggregationLiteral<>(substitution.getTypeSafeSubstitute(resultVariable), aggregator,
				substitution.getTypeSafeSubstitute(inputVariable), getTarget(), substitutedArguments);
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		AggregationLiteral<?, ?> that = (AggregationLiteral<?, ?>) o;
		return resultVariable.equals(that.resultVariable) && inputVariable.equals(that.inputVariable) &&
				aggregator.equals(that.aggregator);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), resultVariable, inputVariable, aggregator);
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
