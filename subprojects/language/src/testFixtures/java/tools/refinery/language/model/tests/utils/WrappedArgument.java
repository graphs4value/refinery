/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
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
