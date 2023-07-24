/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.Action;
import tools.refinery.language.model.problem.AssertionAction;
import tools.refinery.language.model.problem.DeleteAction;
import tools.refinery.language.model.problem.NewAction;
import tools.refinery.language.model.problem.VariableOrNode;

public record WrappedAction(Action action) {
	public Action get() {
		return action;
	}
	
	public VariableOrNode newVar() {
		return ((NewAction) action).getVariable();
	}
	
	public VariableOrNode deleteVar() {
		return ((DeleteAction) action).getVariableOrNode();
	}
	
	public WrappedAtom assertedAtom() {
		return new WrappedAtom(((AssertionAction) action).getAtom());
	}
}
