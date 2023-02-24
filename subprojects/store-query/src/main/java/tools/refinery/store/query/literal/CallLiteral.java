package tools.refinery.store.query.literal;

import tools.refinery.store.query.RelationLike;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class CallLiteral<T extends RelationLike> implements Literal {
	private final CallPolarity polarity;
	private final T target;
	private final List<Variable> arguments;

	protected CallLiteral(CallPolarity polarity, T target, List<Variable> arguments) {
		if (arguments.size() != target.arity()) {
			throw new IllegalArgumentException("%s needs %d arguments, but got %s".formatted(target.name(),
					target.arity(), arguments.size()));
		}
		if (polarity.isTransitive() && target.arity() != 2) {
			throw new IllegalArgumentException("Transitive closures can only take binary relations");
		}
		this.polarity = polarity;
		this.target = target;
		this.arguments = arguments;
	}

	public CallPolarity getPolarity() {
		return polarity;
	}

	public abstract Class<T> getTargetType();

	public T getTarget() {
		return target;
	}

	public List<Variable> getArguments() {
		return arguments;
	}

	@Override
	public void collectAllVariables(Set<Variable> variables) {
		if (polarity.isPositive()) {
			variables.addAll(arguments);
		}
	}

	protected List<Variable> substituteArguments(Substitution substitution) {
		return arguments.stream().map(substitution::getSubstitute).toList();
	}

	/**
	 * Compares the target of this call literal with another object.
	 *
	 * @param helper      Equality helper for comparing {@link Variable} and {@link tools.refinery.store.query.Dnf}
	 *                    instances.
	 * @param otherTarget The object to compare the target to.
	 * @return {@code true} if {@code otherTarget} is equal to the return value of {@link #getTarget()} according to
	 * {@code helper}, {@code false} otherwise.
	 */
	protected boolean targetEquals(LiteralEqualityHelper helper, T otherTarget) {
		return target.equals(otherTarget);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (other.getClass() != getClass()) {
			return false;
		}
		var otherCallLiteral = (CallLiteral<?>) other;
		if (getTargetType() != otherCallLiteral.getTargetType() || polarity != otherCallLiteral.polarity) {
			return false;
		}
		var arity = arguments.size();
		if (arity != otherCallLiteral.arguments.size()) {
			return false;
		}
		for (int i = 0; i < arity; i++) {
			if (!helper.variableEqual(arguments.get(i), otherCallLiteral.arguments.get(i))) {
				return false;
			}
		}
		@SuppressWarnings("unchecked")
		var otherTarget = (T) otherCallLiteral.target;
		return targetEquals(helper, otherTarget);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CallLiteral<?> callAtom = (CallLiteral<?>) o;
		return polarity == callAtom.polarity && Objects.equals(target, callAtom.target) &&
				Objects.equals(arguments, callAtom.arguments);
	}

	@Override
	public int hashCode() {
		return Objects.hash(polarity, target, arguments);
	}

	protected String targetToString() {
		return "@%s %s".formatted(getTargetType().getSimpleName(), target.name());
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		if (!polarity.isPositive()) {
			builder.append("!(");
		}
		builder.append(targetToString());
		if (polarity.isTransitive()) {
			builder.append("+");
		}
		builder.append("(");
		var argumentIterator = arguments.iterator();
		if (argumentIterator.hasNext()) {
			builder.append(argumentIterator.next());
			while (argumentIterator.hasNext()) {
				builder.append(", ").append(argumentIterator.next());
			}
		}
		builder.append(")");
		if (!polarity.isPositive()) {
			builder.append(")");
		}
		return builder.toString();
	}
}
