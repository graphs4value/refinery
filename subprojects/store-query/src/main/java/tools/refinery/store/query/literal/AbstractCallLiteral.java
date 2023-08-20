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
public abstract class AbstractCallLiteral extends AbstractLiteral {
	private final Constraint target;
	private final List<Variable> arguments;
	private final Set<Variable> inArguments;
	private final Set<Variable> outArguments;

	// Use exhaustive switch over enums.
	@SuppressWarnings("squid:S1301")
	protected AbstractCallLiteral(Constraint target, List<Variable> arguments) {
		int arity = target.arity();
		if (arguments.size() != arity) {
			throw new InvalidQueryException("%s needs %d arguments, but got %s".formatted(target.name(),
					target.arity(), arguments.size()));
		}
		this.target = target;
		this.arguments = arguments;
		var mutableInArguments = new LinkedHashSet<Variable>();
		var mutableOutArguments = new LinkedHashSet<Variable>();
		var parameters = target.getParameters();
		for (int i = 0; i < arity; i++) {
			var argument = arguments.get(i);
			var parameter = parameters.get(i);
			if (!parameter.isAssignable(argument)) {
				throw new InvalidQueryException("Argument %d of %s is not assignable to parameter %s"
						.formatted(i, target, parameter));
			}
			switch (parameter.getDirection()) {
			case IN -> {
				mutableOutArguments.remove(argument);
				mutableInArguments.add(argument);
			}
			case OUT -> {
				if (!mutableInArguments.contains(argument)) {
					mutableOutArguments.add(argument);
				}
			}
			}
		}
		inArguments = Collections.unmodifiableSet(mutableInArguments);
		outArguments = Collections.unmodifiableSet(mutableOutArguments);
	}

	public Constraint getTarget() {
		return target;
	}

	public List<Variable> getArguments() {
		return arguments;
	}

	protected Set<Variable> getArgumentsOfDirection(ParameterDirection direction) {
		return switch (direction) {
			case IN -> inArguments;
			case OUT -> outArguments;
		};
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		var inputVariables = new LinkedHashSet<>(getArgumentsOfDirection(ParameterDirection.OUT));
		inputVariables.retainAll(positiveVariablesInClause);
		inputVariables.addAll(getArgumentsOfDirection(ParameterDirection.IN));
		return Collections.unmodifiableSet(inputVariables);
	}

	@Override
	public Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		var privateVariables = new LinkedHashSet<>(getArgumentsOfDirection(ParameterDirection.OUT));
		privateVariables.removeAll(positiveVariablesInClause);
		return Collections.unmodifiableSet(privateVariables);
	}

	@Override
	public Literal substitute(Substitution substitution) {
		var substitutedArguments = arguments.stream().map(substitution::getSubstitute).toList();
		return doSubstitute(substitution, substitutedArguments);
	}

	protected abstract Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments);

	public AbstractCallLiteral withTarget(Constraint newTarget) {
		if (Objects.equals(target, newTarget)) {
			return this;
		}
		return withArguments(newTarget, arguments);
	}

	public abstract AbstractCallLiteral withArguments(Constraint newTarget, List<Variable> newArguments);

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherCallLiteral = (AbstractCallLiteral) other;
		var arity = arguments.size();
		if (arity != otherCallLiteral.arguments.size()) {
			return false;
		}
		for (int i = 0; i < arity; i++) {
			if (!helper.variableEqual(arguments.get(i), otherCallLiteral.arguments.get(i))) {
				return false;
			}
		}
		return target.equals(helper, otherCallLiteral.target);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		int result = super.hashCodeWithSubstitution(helper) * 31 + target.hashCode();
		for (var argument : arguments) {
			result = result * 31 + helper.getVariableHashCode(argument);
		}
		return result;
	}
}
