/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.InvalidQueryException;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalTerms;

import java.util.List;

public class PartialCountTerm extends PartialCallTerm<IntInterval> {
	public PartialCountTerm(Constraint target, List<Variable> arguments) {
		super(IntInterval.class, target, arguments);
	}

	@Override
	protected Term<IntInterval> doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new PartialCountTerm(getTarget(), substitutedArguments);
	}

	@Override
	public Term<IntInterval> withArguments(Constraint newTarget, List<Variable> newArguments) {
		return new PartialCountTerm(newTarget, newArguments);
	}

	@Override
	public Term<IntInterval> reduce() {
		return switch (getTarget().getReduction()) {
			case NOT_REDUCIBLE -> this;
			case ALWAYS_FALSE -> IntIntervalTerms.constant(IntInterval.ZERO);
			case ALWAYS_TRUE -> {
				if (getArguments().isEmpty()) {
					yield IntIntervalTerms.constant(IntInterval.ONE);
				}
				throw new InvalidQueryException("Trying to count an infinite set");
			}
		};
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append("@Partial count");
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
