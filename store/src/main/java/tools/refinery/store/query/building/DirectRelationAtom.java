package tools.refinery.store.query.building;

import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;

public class DirectRelationAtom implements DNFAtom {

	private Relation<TruthValue> relation;
	private List<Variable> substitution;
	private Set<TruthValue> allowedTruthValues;

	public DirectRelationAtom(Relation<TruthValue> relation, List<Variable> substitution,
			Set<TruthValue> allowedTruthValues) {
		this.relation = relation;
		this.substitution = substitution;
		this.allowedTruthValues = allowedTruthValues;
	}
	
	public Relation<TruthValue> getRelation() {
		return relation;
	}
	
	public List<Variable> getSubstitution() {
		return substitution;
	}
	
	public Set<TruthValue> getAllowedTruthValues() {
		return allowedTruthValues;
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
