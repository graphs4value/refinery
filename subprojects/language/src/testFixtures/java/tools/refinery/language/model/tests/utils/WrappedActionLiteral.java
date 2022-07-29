package tools.refinery.language.model.tests.utils;

import tools.refinery.language.model.problem.ActionLiteral;
import tools.refinery.language.model.problem.DeleteActionLiteral;
import tools.refinery.language.model.problem.NewActionLiteral;
import tools.refinery.language.model.problem.ValueActionLiteral;
import tools.refinery.language.model.problem.VariableOrNode;

public record WrappedActionLiteral(ActionLiteral actionLiteral) {
	public ActionLiteral get() {
		return actionLiteral;
	}
	
	public VariableOrNode newVar() {
		return ((NewActionLiteral) actionLiteral).getVariable();
	}
	
	public VariableOrNode deleteVar() {
		return ((DeleteActionLiteral) actionLiteral).getVariableOrNode();
	}
	
	public WrappedAtom valueAtom() {
		return new WrappedAtom(((ValueActionLiteral) actionLiteral).getAtom());
	}
}
