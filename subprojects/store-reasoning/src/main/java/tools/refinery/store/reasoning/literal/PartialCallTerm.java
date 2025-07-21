/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.term.AbstractCallTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.Variable;

import java.util.List;

public abstract class PartialCallTerm<T> extends AbstractCallTerm<T> implements PartialTerm<T> {
	protected PartialCallTerm(Class<T> type, Constraint target, List<Variable> arguments) {
		super(type, target, arguments);
	}

	@Override
	public Term<T> orElseConcreteness(Concreteness fallback) {
		var target = getTarget();
		var modalTarget = ModalConstraint.of(fallback, target);
		return withTarget(modalTarget);
	}

	public ConcretenessSpecification getConcreteness() {
		if (getTarget() instanceof ModalConstraint modalConstraint) {
			return modalConstraint.concreteness();
		}
		return ConcretenessSpecification.UNSPECIFIED;
	}
}
