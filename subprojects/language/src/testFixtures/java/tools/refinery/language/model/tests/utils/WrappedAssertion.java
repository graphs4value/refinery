package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.Assertion;

public record WrappedAssertion(Assertion assertion) {
	public Assertion get() {
		return assertion;
	}

	public WrappedAssertionArgument arg(int i) {
		return new WrappedAssertionArgument(assertion.getArguments().get(i));
	}
}
