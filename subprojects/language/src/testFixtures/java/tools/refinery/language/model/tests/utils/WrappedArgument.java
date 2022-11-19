package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.*;

public record WrappedArgument(Expr expr) {
	public Expr get() {
		return expr;
	}

	public VariableOrNode variableOrNode() {
		return ((VariableOrNodeExpr) expr).getVariableOrNode();
	}

	public Variable variable() {
		return (Variable) variableOrNode();
	}

	public Node node() {
		return (Node) variableOrNode();
	}
}
