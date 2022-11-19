package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.Parameter;
import tools.refinery.language.model.problem.ParametricDefinition;

public interface WrappedParametricDefinition {
	ParametricDefinition get();

	default Parameter param(int i) {
		return get().getParameters().get(i);
	}
}
