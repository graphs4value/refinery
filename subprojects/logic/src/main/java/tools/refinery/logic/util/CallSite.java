/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.util;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.InvalidQueryException;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.ParameterDirection;
import tools.refinery.logic.term.Variable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CallSite {
	private final Constraint target;
	private final List<Variable> arguments;
	private final Set<Variable> inArguments;
	private final Set<Variable> outArguments;

	// Use exhaustive switch over enums.
	@SuppressWarnings("squid:S1301")
	public CallSite(Constraint target, List<Variable> arguments) {
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

	public Set<Variable> getArgumentsOfDirection(ParameterDirection direction) {
		return switch (direction) {
			case IN -> inArguments;
			case OUT -> outArguments;
		};
	}

	public Set<Variable> getInputVariablesForNonEnumerableCall(Set<? extends Variable> positiveVariablesInClause) {
		var inputVariables = new LinkedHashSet<>(getArgumentsOfDirection(ParameterDirection.OUT));
		inputVariables.retainAll(positiveVariablesInClause);
		inputVariables.addAll(getArgumentsOfDirection(ParameterDirection.IN));
		return Collections.unmodifiableSet(inputVariables);
	}

	public Set<Variable> getPrivateVariablesForNonEnumerableCall(Set<? extends Variable> positiveVariablesInClause) {
		var privateVariables = new LinkedHashSet<>(getArgumentsOfDirection(ParameterDirection.OUT));
		privateVariables.removeAll(positiveVariablesInClause);
		return Collections.unmodifiableSet(privateVariables);
	}

	public List<Variable> getSubstitutedArguments(Substitution substitution) {
		return getArguments().stream().map(substitution::getSubstitute).toList();
	}

	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, CallSite other) {
		var arity = arguments.size();
		if (arity != other.arguments.size()) {
			return false;
		}
		for (int i = 0; i < arity; i++) {
			if (!helper.variableEqual(arguments.get(i), other.arguments.get(i))) {
				return false;
			}
		}
		return target.equals(helper, other.target);
	}

	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		int result = target.hashCode();
		for (var argument : arguments) {
			result = result * 31 + helper.getVariableHashCode(argument);
		}
		return result;
	}
}
