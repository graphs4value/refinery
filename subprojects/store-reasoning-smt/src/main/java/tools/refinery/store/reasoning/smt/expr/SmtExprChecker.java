/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.expr;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;
import tools.refinery.logic.term.abstractdomain.*;
import tools.refinery.logic.term.int_.RealToIntTerm;
import tools.refinery.logic.term.operators.*;
import tools.refinery.logic.term.real.IntToRealTerm;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;
import tools.refinery.store.reasoning.literal.PartialFunctionCallTerm;

import java.math.BigDecimal;
import java.math.BigInteger;

public class SmtExprChecker {
	// Use raw types to avoid having to check and capture specific sorts that are already validated by the Term data
	// structure and types.
	public boolean isSupported(Term<?> term) {
		return switch (term) {
			case ConstantTerm<?> constantTerm -> isSupported(constantTerm);
			case PartialFunctionCallTerm<?, ?> partialFunctionCallTerm -> isSupported(partialFunctionCallTerm);
			case UnaryTerm<?, ?> unaryTerm -> switch (unaryTerm) {
				case NotTerm<?> _, PlusTerm<?> _, MinusTerm<?> _, IntToRealTerm _, RealToIntTerm _ ->
						isSupported(unaryTerm.getBody());
				default -> false;
			};
			case BinaryTerm<?, ?, ?> binaryTerm -> switch (binaryTerm) {
				case AbstractDomainEqTerm<?, ?> _, AbstractDomainNotEqTerm<?, ?> _,
				     AbstractDomainGreaterEqTerm<?, ?> _, AbstractDomainGreaterTerm<?, ?> _,
				     AbstractDomainLessEqTerm<?, ?> _, AbstractDomainLessTerm<?, ?> _,
				     AndTerm<?> _, OrTerm<?> _, XorTerm<?> _, AddTerm<?> _, SubTerm<?> _, MulTerm<?> _,
				     DivTerm<?> _, PowTerm<?> _ ->
						isSupported(binaryTerm.getLeft()) && isSupported(binaryTerm.getRight());
				default -> false;
			};
			default -> false;
		};
	}

	private boolean isSupported(ConstantTerm<?> term) {
		var value = term.getValue();
		if (!(value instanceof AbstractValue<?, ?> abstractValue) || !abstractValue.isConcrete()) {
			return false;
		}
		var concreteValue = abstractValue.getArbitrary();
		return switch (concreteValue) {
			case Boolean _, BigInteger _, BigDecimal _, String _ -> true;
			case null, default -> false;
		};
	}

	private boolean isSupported(PartialFunctionCallTerm<?, ?> partialFunctionCallTerm) {
		return partialFunctionCallTerm.getConcreteness() == ConcretenessSpecification.UNSPECIFIED;
	}
}
