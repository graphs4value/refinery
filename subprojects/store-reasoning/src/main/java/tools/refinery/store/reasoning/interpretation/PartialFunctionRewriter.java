package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.literal.AbstractCallLiteral;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.AbstractCallTerm;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;

import java.util.List;
import java.util.Set;

@FunctionalInterface
public interface PartialFunctionRewriter<A extends AbstractValue<A, C>, C> {
	Term<A> rewritePartialFunctionCall(Concreteness concreteness, List<NodeVariable> arguments);
}
