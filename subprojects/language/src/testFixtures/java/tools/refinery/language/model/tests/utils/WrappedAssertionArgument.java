package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.AssertionArgument;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.NodeAssertionArgument;

public record WrappedAssertionArgument(AssertionArgument assertionArgument) {
	public AssertionArgument get() {
		return assertionArgument;
	}
	
	public Node node() {
		return ((NodeAssertionArgument) assertionArgument).getNode();
	}
}
