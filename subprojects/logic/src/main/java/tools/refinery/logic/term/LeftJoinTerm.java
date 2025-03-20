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

import java.util.*;

// {@link Object#equals(Object)} is implemented by {@link AbstractTerm}.
@SuppressWarnings("squid:S2160")
public class LeftJoinTerm<T> extends AbstractCallTerm<T> {
	private final DataVariable<T> placeholderVariable;
	private final T defaultValue;

	public LeftJoinTerm(DataVariable<T> placeholderVariable, T defaultValue, Constraint target,
						List<Variable> arguments) {
		super(placeholderVariable.getType(), target, arguments);
		this.placeholderVariable = placeholderVariable;
		this.defaultValue = defaultValue;
		if (defaultValue == null) {
			throw new InvalidQueryException("Default value must not be null");
		}
		if (!getType().isInstance(defaultValue)) {
			throw new InvalidQueryException("Default value %s must be assignable to type %s"
					.formatted(defaultValue, getType().getName()));
		}
		if (!getArgumentsOfDirection(ParameterDirection.OUT).contains(placeholderVariable)) {
			throw new InvalidQueryException(
					"Placeholder variable %s must be bound with direction %s in the argument list"
							.formatted(placeholderVariable, ParameterDirection.OUT));
		}
	}

	public DataVariable<T> getPlaceholderVariable() {
		return placeholderVariable;
	}

	public T getDefaultValue() {
		return defaultValue;
	}

	@Override
	protected Term<T> doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new LeftJoinTerm<>(substitution.getTypeSafeSubstitute(placeholderVariable), defaultValue, getTarget(),
				substitutedArguments);
	}

	@Override
	public Term<T> withArguments(Constraint newTarget, List<Variable> newArguments) {
		return new LeftJoinTerm<>(placeholderVariable, defaultValue, newTarget, newArguments);
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		if (positiveVariablesInClause.contains(placeholderVariable)) {
			throw new InvalidQueryException("Placeholder variable %s must not be bound".formatted(placeholderVariable));
		}
		var inputVariables = new LinkedHashSet<>(getArguments());
		inputVariables.remove(placeholderVariable);
		return Collections.unmodifiableSet(inputVariables);
	}

	@Override
	public Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		return Set.of(placeholderVariable);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, AnyTerm other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherleftJoinTerm = (LeftJoinTerm<?>) other;
		return helper.variableEqual(placeholderVariable, otherleftJoinTerm.placeholderVariable) &&
				Objects.equals(defaultValue, otherleftJoinTerm.defaultValue);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), helper.getVariableHashCode(placeholderVariable),
				defaultValue);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append(getTarget().toReferenceString());
		builder.append("(");
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
			// No result variable to show.
			builder.append("@Default(").append(defaultValue).append(")");
		} else {
			builder.append(argument);
		}
	}
}
