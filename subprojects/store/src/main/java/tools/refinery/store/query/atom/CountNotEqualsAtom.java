package tools.refinery.store.query.atom;

import tools.refinery.store.model.RelationLike;
import tools.refinery.store.query.Variable;

import java.util.List;
import java.util.Set;

public record CountNotEqualsAtom<T extends RelationLike>(boolean must, int threshold, T mayTarget, T mustTarget,
														 List<Variable> substitution) implements DNFAtom {
	public CountNotEqualsAtom {
		if (substitution.size() != mayTarget.getArity()) {
			throw new IllegalArgumentException("%s needs %d arguments, but got %s".formatted(mayTarget.getName(),
					mayTarget.getArity(), substitution.size()));
		}
		if (substitution.size() != mustTarget.getArity()) {
			throw new IllegalArgumentException("%s needs %d arguments, but got %s".formatted(mustTarget.getName(),
					mustTarget.getArity(), substitution.size()));
		}
	}

	public CountNotEqualsAtom(boolean must, int threshold, T mayTarget, T mustTarget, Variable... substitution) {
		this(must, threshold, mayTarget, mustTarget, List.of(substitution));
	}

	@Override
	public void collectAllVariables(Set<Variable> variables) {
		// No variables to collect, because all variables should either appear in other clauses,
		// or are quantified by this clause.
	}
}
