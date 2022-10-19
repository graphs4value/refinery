package tools.refinery.store.query.atom;

import tools.refinery.store.model.RelationLike;
import tools.refinery.store.query.Variable;

import java.util.List;
import java.util.Set;

public abstract class AbstractCallAtom<T extends RelationLike> extends AbstractSubstitutionAtom<T> {
	private final CallKind kind;

	protected AbstractCallAtom(CallKind kind, T target, List<Variable> substitution) {
		super(target, substitution);
		if (kind.isTransitive() && target.getArity() != 2) {
			throw new IllegalArgumentException("Transitive closures can only take binary relations");
		}
		this.kind = kind;
	}

	public CallKind getKind() {
		return kind;
	}

	@Override
	public void collectAllVariables(Set<Variable> variables) {
		if (kind.isPositive()) {
			super.collectAllVariables(variables);
		}
	}
}
