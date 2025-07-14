/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.AbstractCallTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueTerms;

import java.util.List;

public class ReifyTerm extends AbstractCallTerm<TruthValue> {
	public ReifyTerm(Constraint target, List<Variable> arguments) {
		super(TruthValue.class, target, arguments);
	}

	@Override
	protected Term<TruthValue> doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new ReifyTerm(getTarget(), substitutedArguments);
	}

	@Override
	public Term<TruthValue> withArguments(Constraint newTarget, List<Variable> newArguments) {
		return new ReifyTerm(newTarget, newArguments);
	}

	@Override
	public Term<TruthValue> reduce() {
		return switch (getTarget().getReduction()) {
			case NOT_REDUCIBLE -> this;
			case ALWAYS_FALSE -> TruthValueTerms.constant(TruthValue.FALSE);
			case ALWAYS_TRUE -> TruthValueTerms.constant(TruthValue.TRUE);
		};
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append("@Reify");
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
