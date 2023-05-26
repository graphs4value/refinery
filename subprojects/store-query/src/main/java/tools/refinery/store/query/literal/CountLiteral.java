/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.term.int_.IntTerms;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CountLiteral extends AbstractCallLiteral {
	private final DataVariable<Integer> resultVariable;

	public CountLiteral(DataVariable<Integer> resultVariable, Constraint target, List<Variable> arguments) {
		super(target, arguments);
		if (!resultVariable.getType().equals(Integer.class)) {
			throw new IllegalArgumentException("Count result variable %s must be of type %s, got %s instead".formatted(
					resultVariable, Integer.class.getName(), resultVariable.getType().getName()));
		}
		if (arguments.contains(resultVariable)) {
			throw new IllegalArgumentException("Count result variable %s must not appear in the argument list"
					.formatted(resultVariable));
		}
		this.resultVariable = resultVariable;
	}

	public DataVariable<Integer> getResultVariable() {
		return resultVariable;
	}

	@Override
	public Set<Variable> getOutputVariables() {
		return Set.of(resultVariable);
	}

	@Override
	public Literal reduce() {
		var reduction = getTarget().getReduction();
		return switch (reduction) {
			case ALWAYS_FALSE -> getResultVariable().assign(IntTerms.constant(0));
			// The only way a constant {@code true} predicate can be called in a negative position is to have all of
			// its arguments bound as input variables. Thus, there will only be a single match.
			case ALWAYS_TRUE -> getResultVariable().assign(IntTerms.constant(1));
			case NOT_REDUCIBLE -> this;
		};
	}

	@Override
	protected Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new CountLiteral(substitution.getTypeSafeSubstitute(resultVariable), getTarget(), substitutedArguments);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherCountLiteral = (CountLiteral) other;
		return helper.variableEqual(resultVariable, otherCountLiteral.resultVariable);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		CountLiteral that = (CountLiteral) o;
		return resultVariable.equals(that.resultVariable);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), resultVariable);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append(resultVariable);
		builder.append(" is count ");
		builder.append(getTarget().toReferenceString());
		builder.append("(");
		var argumentIterator = getArguments().iterator();
		if (argumentIterator.hasNext()) {
			builder.append(argumentIterator.next());
			while (argumentIterator.hasNext()) {
				builder.append(", ").append(argumentIterator.next());
			}
		}
		builder.append(")");
		return builder.toString();
	}
}
