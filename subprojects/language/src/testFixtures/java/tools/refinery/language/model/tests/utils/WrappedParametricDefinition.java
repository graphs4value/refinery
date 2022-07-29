package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.Parameter;
import tools.refinery.language.model.problem.ParametricDefinition;

public interface WrappedParametricDefinition {
	public ParametricDefinition get();
	
	public default Parameter param(int i) {
		return get().getParameters().get(i);
	}

	public default WrappedConjunction conj(int i) {
		return new WrappedConjunction(get().getBodies().get(i));
	}
}
