package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.*;

public record WrappedArgument(Argument argument) {
	public Argument get() {
		return argument;
	}

	public VariableOrNode variableOrNode() {
		return ((VariableOrNodeArgument) argument).getVariableOrNode();
	}

	public Variable variable() {
		return (Variable) variableOrNode();
	}

	public Node node() {
		return (Node) variableOrNode();
	}
}
