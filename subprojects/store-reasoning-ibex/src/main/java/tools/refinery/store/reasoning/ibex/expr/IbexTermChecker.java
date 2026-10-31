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
	public boolean isSupported(Term<TruthValue> term) {
		return check(term).isSupported();
	}

	private TermSupport check(Term<TruthValue> term) {
		return switch (term) {
			case AbstractDomainLessEqTerm<?, ?> _, AbstractDomainGreaterEqTerm<?, ?> _,
			     AbstractDomainLessTerm<?, ?> _, AbstractDomainGreaterTerm<?, ?> _, AbstractDomainEqTerm<?, ?> _ -> {
				var binaryTerm = (BinaryTerm<TruthValue, ?, ?>) term;
				yield checkArith(binaryTerm.getLeft()).and(checkArith(binaryTerm.getRight()));
			}
			case null, default -> TermSupport.INVALID;
		};
	}

	private TermSupport checkArith(Term<?> term) {
		return switch (term) {
			case ConstantTerm<?> c -> checkConstant(c);
			case PartialFunctionCallTerm<?, ?> pf -> checkCall(pf);
			case PlusTerm<?> _, MinusTerm<?> _ -> checkArith(((UnaryTerm<?, ?>) term).getBody());
			case AddTerm<?> _, SubTerm<?> _, MulTerm<?> _, DivTerm<?> _, PowTerm<?> _ -> {
				var binaryTerm = (BinaryTerm<?, ?, ?>) term;
				yield checkArith(binaryTerm.getLeft()).and(checkArith(binaryTerm.getRight()));
			}
			case null, default -> TermSupport.INVALID;
		};
	}

	private TermSupport checkConstant(ConstantTerm<?> term) {
		return switch (term.getValue()) {
			case IntInterval iv when iv.isConcrete() -> TermSupport.NO_VARIABLES;
			case RealInterval iv when iv.isConcrete() -> TermSupport.NO_VARIABLES;
			case null, default -> TermSupport.INVALID;
		};
	}

	private TermSupport checkCall(PartialFunctionCallTerm<?, ?> term) {
		var domain = term.getPartialFunction().abstractDomain();
		if (IntIntervalDomain.INSTANCE.equals(domain) || RealIntervalDomain.INSTANCE.equals(domain)) {
			return TermSupport.SUPPORTED;
		}
		return TermSupport.INVALID;
	}

	private enum TermSupport {
		/**
		 * Passing expressions with no variables to IBEX makes it crash on Windows.
		 */
		NO_VARIABLES,
		SUPPORTED,
		INVALID;

		public boolean isSupported() {
			return this == SUPPORTED;
		}

		public TermSupport and(TermSupport other) {
			if (this == INVALID || other == INVALID) {
				return INVALID;
			}
			if (this == NO_VARIABLES && other == NO_VARIABLES) {
				return NO_VARIABLES;
			}
			return SUPPORTED;
		}
	}
}
