package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.Argument;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.model.problem.Variable;
import tools.refinery.language.model.problem.VariableOrNode;
import tools.refinery.language.model.problem.VariableOrNodeArgument;

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
