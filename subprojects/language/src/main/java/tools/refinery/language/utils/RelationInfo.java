package tools.refinery.language.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import tools.refinery.language.model.problem.Assertion;
import tools.refinery.language.model.problem.Conjunction;
import tools.refinery.language.model.problem.Multiplicity;
import tools.refinery.language.model.problem.Parameter;
import tools.refinery.language.model.problem.PredicateKind;
import tools.refinery.language.model.problem.Relation;
import tools.refinery.language.model.problem.TypeScope;

public record RelationInfo(String name, PredicateKind kind, List<Parameter> parameters, Multiplicity multiplicity,
		Relation opposite, Collection<Conjunction> bodies, Collection<Assertion> defaultAssertions,
		Collection<Assertion> assertions, Collection<TypeScope> typeScopes) {
	public RelationInfo(String name, PredicateKind kind, List<Parameter> parameters, Multiplicity multiplicity,
			Relation opposite, Collection<Conjunction> bodies) {
		this(name, kind, parameters, multiplicity, opposite, bodies, new ArrayList<>(), new ArrayList<>(),
				new ArrayList<>());
	}

	public boolean hasDefinition() {
		return bodies != null && !bodies.isEmpty();
	}
}
