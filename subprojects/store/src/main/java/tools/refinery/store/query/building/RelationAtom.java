package tools.refinery.store.query.building;

import tools.refinery.store.query.view.RelationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record RelationAtom(RelationView<?> view, List<Variable> substitution) implements DNFAtom {
	public RelationAtom(RelationView<?> view, List<Variable> substitution) {
		this.view = view;
		// Create a mutable copy of substitution so that unifyVariables can change it.
		this.substitution = new ArrayList<>(substitution);
	}

	@Override
	public void unifyVariables(Map<String, Variable> variables) {
		for (int i = 0; i < this.substitution.size(); i++) {
			final Object term = this.substitution.get(i);
			if (term instanceof Variable variableReference) {
				this.substitution.set(i, DNFAtom.unifyVariables(variables, variableReference));
			}
		}
	}

	@Override
	public void collectAllVariables(Set<Variable> variables) {
		DNFAtom.addToCollection(variables, substitution);
	}
}
