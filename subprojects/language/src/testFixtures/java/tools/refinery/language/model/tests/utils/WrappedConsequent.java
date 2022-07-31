package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.Consequent;

public record WrappedConsequent(Consequent consequent) {
	public Consequent get() {
		return consequent;
	}

	public WrappedAction action(int i) {
		return new WrappedAction(consequent.getActions().get(i));
	}
}
