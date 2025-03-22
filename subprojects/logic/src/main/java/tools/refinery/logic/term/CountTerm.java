/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.InvalidQueryException;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.int_.IntTerms;

import java.util.List;

public class CountTerm extends AbstractCallTerm<Integer> {
	public CountTerm(Constraint target, List<Variable> arguments) {
		super(Integer.class, target, arguments);
	}

	@Override
	protected Term<Integer> doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new CountTerm(getTarget(), substitutedArguments);
	}

	@Override
	public Term<Integer> withArguments(Constraint newTarget, List<Variable> newArguments) {
		return new CountTerm(newTarget, newArguments);
	}

	@Override
	public Term<Integer> reduce() {
		return switch (getTarget().getReduction()) {
			case NOT_REDUCIBLE -> this;
			case ALWAYS_FALSE -> IntTerms.constant(0);
			case ALWAYS_TRUE -> {
				if (getArguments().isEmpty()) {
					yield IntTerms.constant(1);
				}
				throw new InvalidQueryException("Trying to count an infinite set");
			}
		};
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append("count");
		builder.append(' ');
		builder.append(getTarget().toReferenceString());
		builder.append('(');
		var argumentIterator = getArguments().iterator();
		if (argumentIterator.hasNext()) {
			builder.append(argumentIterator.next());
			while (argumentIterator.hasNext()) {
				builder.append(", ").append(argumentIterator.next());
			}
		}
		builder.append(')');
		return builder.toString();
	}
}
