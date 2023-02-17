package tools.refinery.store.query.literal;

import tools.refinery.store.query.DnfUtils;
import tools.refinery.store.query.RelationLike;
import tools.refinery.store.query.Variable;

import java.util.List;
import java.util.Map;
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

	protected List<Variable> substituteArguments(Map<Variable, Variable> substitution) {
		return arguments.stream().map(variable -> DnfUtils.maybeSubstitute(variable, substitution)).toList();
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
}
