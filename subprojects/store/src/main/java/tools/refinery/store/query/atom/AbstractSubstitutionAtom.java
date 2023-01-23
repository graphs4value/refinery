package tools.refinery.store.query.atom;

import tools.refinery.store.representation.SymbolLike;
import tools.refinery.store.query.Variable;

import java.util.List;
import java.util.Set;

public abstract class AbstractSubstitutionAtom<T extends SymbolLike> implements DNFAtom {
	private final T target;

	private final List<Variable> substitution;

	protected AbstractSubstitutionAtom(T target, List<Variable> substitution) {
		if (substitution.size() != target.arity()) {
			throw new IllegalArgumentException("%s needs %d arguments, but got %s".formatted(target.name(),
					target.arity(), substitution.size()));
		}
		this.target = target;
		this.substitution = substitution;
	}

	public T getTarget() {
		return target;
	}

	public List<Variable> getSubstitution() {
		return substitution;
	}

	@Override
	public void collectAllVariables(Set<Variable> variables) {
		variables.addAll(substitution);
	}
}
