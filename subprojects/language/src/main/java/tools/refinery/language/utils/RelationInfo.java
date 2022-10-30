package tools.refinery.language.utils;

import tools.refinery.language.model.problem.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record RelationInfo(String name, ContainmentRole containmentRole, List<Parameter> parameters,
						   Multiplicity multiplicity, Relation opposite, Collection<Conjunction> bodies,
						   Collection<Assertion> assertions, Collection<TypeScope> typeScopes) {
	public RelationInfo(String name, ContainmentRole containmentRole, List<Parameter> parameters,
						Multiplicity multiplicity, Relation opposite, Collection<Conjunction> bodies) {
		this(name, containmentRole, parameters, multiplicity, opposite, bodies, new ArrayList<>(), new ArrayList<>());
	}

	public boolean hasDefinition() {
		return bodies != null && !bodies.isEmpty();
	}

	public int arity() {
		return parameters.size();
	}
}
