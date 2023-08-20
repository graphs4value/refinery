/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.InvalidQueryException;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.equality.LiteralHashCodeHelper;
import tools.refinery.store.query.term.ConstantTerm;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.Variable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

// {@link Object#equals(Object)} is implemented by {@link AbstractLiteral}.
@SuppressWarnings("squid:S2160")
public abstract class AbstractCountLiteral<T> extends AbstractCallLiteral {
	private final Class<T> resultType;
	private final DataVariable<T> resultVariable;

	protected AbstractCountLiteral(Class<T> resultType, DataVariable<T> resultVariable, Constraint target,
								   List<Variable> arguments) {
		super(target, arguments);
		if (!resultVariable.getType().equals(resultType)) {
			throw new InvalidQueryException("Count result variable %s must be of type %s, got %s instead".formatted(
					resultVariable, resultType, resultVariable.getType().getName()));
		}
		if (arguments.contains(resultVariable)) {
			throw new InvalidQueryException("Count result variable %s must not appear in the argument list"
					.formatted(resultVariable));
		}
		this.resultType = resultType;
		this.resultVariable = resultVariable;
	}

	public Class<T> getResultType() {
		return resultType;
	}

	public DataVariable<T> getResultVariable() {
		return resultVariable;
	}

	@Override
	public Set<Variable> getOutputVariables() {
		return Set.of(resultVariable);
	}

	protected abstract T zero();

	protected abstract T one();

	@Override
	public Literal reduce() {
		var reduction = getTarget().getReduction();
		return switch (reduction) {
			case ALWAYS_FALSE -> getResultVariable().assign(new ConstantTerm<>(resultType, zero()));
			// The only way a constant {@code true} predicate can be called in a negative position is to have all of
			// its arguments bound as input variables. Thus, there will only be a single match.
			case ALWAYS_TRUE -> getResultVariable().assign(new ConstantTerm<>(resultType, one()));
			case NOT_REDUCIBLE -> this;
		};
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherCountLiteral = (AbstractCountLiteral<?>) other;
		return Objects.equals(resultType, otherCountLiteral.resultType) &&
				helper.variableEqual(resultVariable, otherCountLiteral.resultVariable);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return Objects.hash(super.hashCodeWithSubstitution(helper), resultType,
				helper.getVariableHashCode(resultVariable));
	}

	protected abstract String operatorName();

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append(resultVariable);
		builder.append(" is ");
		builder.append(operatorName());
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
