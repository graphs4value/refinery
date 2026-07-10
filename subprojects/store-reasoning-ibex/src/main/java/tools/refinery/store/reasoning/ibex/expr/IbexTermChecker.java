/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.ibex.expr;

import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;
import tools.refinery.logic.term.abstractdomain.*;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.operators.*;
import tools.refinery.logic.term.realinterval.RealInterval;
import tools.refinery.logic.term.realinterval.RealIntervalDomain;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.literal.PartialFunctionCallTerm;

public class IbexTermChecker {
	/**
	 * Converts the top-level comparison term to an IBEX constraint string.
	 *
	 * @param term The term to convert.
	 * @return The converted constraint string.
	 */
	public boolean isSupported(Term<TruthValue> term) {
		return switch (term) {
			case AbstractDomainLessEqTerm<?, ?> _, AbstractDomainGreaterEqTerm<?, ?> _,
			     AbstractDomainLessTerm<?, ?> _, AbstractDomainGreaterTerm<?, ?> _, AbstractDomainEqTerm<?, ?> _ -> {
				var binaryTerm = (BinaryTerm<TruthValue, ?, ?>) term;
				yield isArithSupported(binaryTerm.getLeft()) && isArithSupported(binaryTerm.getRight());
			}
			case null, default -> false;
		};
	}

	private boolean isArithSupported(Term<?> term) {
		return switch (term) {
			case ConstantTerm<?> c -> isConstantSupported(c);
			case PartialFunctionCallTerm<?, ?> pf -> isCallSupported(pf);
			case PlusTerm<?> _, MinusTerm<?> _ -> isArithSupported(((UnaryTerm<?, ?>) term).getBody());
			case AddTerm<?> _, SubTerm<?> _, MulTerm<?> _, DivTerm<?> _, PowTerm<?> _ -> {
				var binaryTerm = (BinaryTerm<?, ?, ?>) term;
				yield isArithSupported(binaryTerm.getLeft()) && isArithSupported(binaryTerm.getRight());
			}
			case null, default -> throw new IllegalArgumentException("Unsupported arithmetic term: " + term);
		};
	}

	private boolean isConstantSupported(ConstantTerm<?> term) {
		return switch (term.getValue()) {
			case IntInterval iv when iv.isConcrete() -> true;
			case RealInterval iv when iv.isConcrete() -> true;
			case null, default -> false;
		};
	}

	private boolean isCallSupported(PartialFunctionCallTerm<?, ?> term) {
		var domain = term.getPartialFunction().abstractDomain();
		return IntIntervalDomain.INSTANCE.equals(domain) || RealIntervalDomain.INSTANCE.equals(domain);
	}
}
