/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.ibex.expr;

import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.abstractdomain.*;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.operators.*;
import tools.refinery.logic.term.realinterval.RealInterval;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.ibex.internal.PreparedIbexRule.Influence;
import tools.refinery.store.reasoning.literal.PartialFunctionCallTerm;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public class TermToIbexConstraint {
	private final List<Influence> influences;
	private final ObjectIntMap<NodeVariable> parameterMap;

	public TermToIbexConstraint(List<Influence> influences, ObjectIntMap<NodeVariable> parameterMap) {
		this.influences = influences;
		this.parameterMap = parameterMap;
	}

	/**
	 * Converts the top-level comparison term to an IBEX constraint string.
	 *
	 * @param term The term to convert.
	 * @return The converted constraint string.
	 */
	public String toConstraint(Term<TruthValue> term) {
		return switch (term) {
			case AbstractDomainLessEqTerm<?, ?> t ->
					toArith(t.getLeft()) + "<=" + toArith(t.getRight());
			case AbstractDomainGreaterEqTerm<?, ?> t ->
					toArith(t.getLeft()) + ">=" + toArith(t.getRight());
			case AbstractDomainLessTerm<?, ?> t ->
					toArith(t.getLeft()) + "<" + toArith(t.getRight());
			case AbstractDomainGreaterTerm<?, ?> t ->
					toArith(t.getLeft()) + ">" + toArith(t.getRight());
			case AbstractDomainEqTerm<?, ?> t ->
					toArith(t.getLeft()) + "=" + toArith(t.getRight());
			default -> throw new IllegalArgumentException(
					"IBEX requires a single arithmetic comparison at the rule top level, got: " + term);
		};
	}

	private String toArith(Term<?> term) {
		return switch (term) {
			case ConstantTerm<?> c -> toConstant(c);
			case PartialFunctionCallTerm<?, ?> pf -> toVariable(pf);
			case PlusTerm<?> u -> toArith(u.getBody());
			case MinusTerm<?> u -> "(-" + toArith(u.getBody()) + ")";
			case AddTerm<?> b -> "(" + toArith(b.getLeft()) + "+" + toArith(b.getRight()) + ")";
			case SubTerm<?> b -> "(" + toArith(b.getLeft()) + "-" + toArith(b.getRight()) + ")";
			case MulTerm<?> b -> "(" + toArith(b.getLeft()) + "*" + toArith(b.getRight()) + ")";
			case DivTerm<?> b -> "(" + toArith(b.getLeft()) + "/" + toArith(b.getRight()) + ")";
			case PowTerm<?> b -> "(" + toArith(b.getLeft()) + "^" + toArith(b.getRight()) + ")";
			default -> throw new IllegalArgumentException("Unsupported arithmetic term: " + term);
		};
	}

	private String toConstant(ConstantTerm<?> term) {
		var value = term.getValue();
		return switch (value) {
			case IntInterval iv when iv.isConcrete() -> {
				var concrete = iv.getConcrete();
				if (concrete == null) {
					throw new IllegalArgumentException("Concrete IntInterval has null value");
				}
				yield concrete.toString();
			}
			case RealInterval iv when iv.isConcrete() -> {
				var concrete = iv.getConcrete();
				if (concrete == null) {
					throw new IllegalArgumentException("Concrete RealInterval has null value");
				}
				yield concrete.toString();
			}
			default -> throw new IllegalArgumentException(
					"Unsupported constant type for IBEX constraint: " + value);
		};
	}

	private String toVariable(PartialFunctionCallTerm<?, ?> term) {
		var partialFunction = term.getPartialFunction();
		var arguments = term.getArguments();
		var paramIndices = new int[arguments.size()];
		for (int i = 0; i < paramIndices.length; i++) {
			paramIndices[i] = parameterMap.get(arguments.get(i));
		}
		var influence = new Influence(partialFunction, Tuple.of(paramIndices));
		int idx = influences.indexOf(influence);
		if (idx < 0) {
			throw new IllegalArgumentException(
					"Partial function call not found in influences: " + influence);
		}
		return "{" + idx + "}";
	}
}
