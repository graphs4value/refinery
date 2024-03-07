/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.literal;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.InvalidQueryException;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.DataVariable;
import tools.refinery.logic.term.ParameterDirection;
import tools.refinery.logic.term.Variable;

import java.util.*;

// {@link Object#equals(Object)} is implemented by {@link AbstractLiteral}.
@SuppressWarnings("squid:S2160")
public class LeftJoinLiteral<T> extends AbstractCallLiteral {
	private final DataVariable<T> resultVariable;
	private final DataVariable<T> placeholderVariable;
	private final T defaultValue;

	public LeftJoinLiteral(DataVariable<T> resultVariable, DataVariable<T> placeholderVariable,
						   T defaultValue, Constraint target, List<Variable> arguments) {
		super(target, arguments);
		this.resultVariable = resultVariable;
		this.placeholderVariable = placeholderVariable;
		this.defaultValue = defaultValue;
		if (defaultValue == null) {
			throw new InvalidQueryException("Default value must not be null");
		}
		if (!resultVariable.getType().isInstance(defaultValue)) {
			throw new InvalidQueryException("Default value %s must be assignable to result variable %s type %s"
					.formatted(defaultValue, resultVariable, resultVariable.getType().getName()));
		}
		if (!getArgumentsOfDirection(ParameterDirection.OUT).contains(placeholderVariable)) {
			throw new InvalidQueryException(
					"Placeholder variable %s must be bound with direction %s in the argument list"
							.formatted(resultVariable, ParameterDirection.OUT));
		}
		if (arguments.contains(resultVariable)) {
			throw new InvalidQueryException("Result variable must not appear in the argument list");
		}
	}

	public DataVariable<T> getResultVariable() {
		return resultVariable;
	}

	public DataVariable<T> getPlaceholderVariable() {
		return placeholderVariable;
	}

	public T getDefaultValue() {
		return defaultValue;
	}

	@Override
	public Set<Variable> getOutputVariables() {
		return Set.of(resultVariable);
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		var inputVariables = new LinkedHashSet<>(getArguments());
		inputVariables.remove(placeholderVariable);
		return Collections.unmodifiableSet(inputVariables);
	}

	@Override
	public Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		return Set.of(placeholderVariable);
	}

	@Override
	public Literal reduce() {
		var reduction = getTarget().getReduction();
		return switch (reduction) {
			case ALWAYS_FALSE -> resultVariable.assign(new ConstantTerm<>(resultVariable.getType(), defaultValue));
			case ALWAYS_TRUE -> throw new InvalidQueryException("Trying to left join an infinite set");
			case NOT_REDUCIBLE -> this;
		};
	}

	@Override
	protected Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new LeftJoinLiteral<>(substitution.getTypeSafeSubstitute(resultVariable),
				substitution.getTypeSafeSubstitute(placeholderVariable), defaultValue, getTarget(),
				substitutedArguments);
	}

	@Override
	public AbstractCallLiteral withArguments(Constraint newTarget, List<Variable> newArguments) {
		return new LeftJoinLiteral<>(resultVariable, placeholderVariable, defaultValue, newTarget, newArguments);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherLeftJoinLiteral = (LeftJoinLiteral<?>) other;
		return helper.variableEqual(resultVariable, otherLeftJoinLiteral.resultVariable) &&
				helper.variableEqual(placeholderVariable, otherLeftJoinLiteral.placeholderVariable) &&
				Objects.equals(defaultValue, otherLeftJoinLiteral.defaultValue);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), helper.getVariableHashCode(resultVariable),
				helper.getVariableHashCode(placeholderVariable), defaultValue);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		var argumentIterator = getArguments().iterator();
		if (argumentIterator.hasNext()) {
			appendArgument(builder, argumentIterator.next());
			while (argumentIterator.hasNext()) {
				builder.append(", ");
				appendArgument(builder, argumentIterator.next());
			}
		}
		builder.append(")");
		return builder.toString();
	}

	private void appendArgument(StringBuilder builder, Variable argument) {
		if (placeholderVariable.equals(argument)) {
			builder.append("@Default(").append(defaultValue).append(") ");
			argument = resultVariable;
		}
		builder.append(argument);
	}
}
