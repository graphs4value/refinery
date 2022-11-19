package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.PredicateDefinition;

public record WrappedPredicateDefinition(PredicateDefinition predicateDefinition)
		implements WrappedParametricDefinition {
	@Override
	public PredicateDefinition get() {
		return predicateDefinition;
	}

	public WrappedConjunction conj(int i) {
		return new WrappedConjunction(predicateDefinition.getBodies().get(i));
	}
}
