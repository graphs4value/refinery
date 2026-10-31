/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.smt.expr;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.*;
import tools.refinery.logic.term.abstractdomain.*;
import tools.refinery.logic.term.int_.RealToIntTerm;
import tools.refinery.logic.term.operators.*;
import tools.refinery.logic.term.real.IntToRealTerm;
import tools.refinery.logic.term.string.StringValue;
import tools.refinery.store.reasoning.literal.ConcretenessSpecification;
import tools.refinery.store.reasoning.literal.PartialFunctionCallTerm;
import tools.refinery.store.reasoning.smt.internal.context.ModelContext;
import tools.refinery.store.tuple.Tuple;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TermToExpr {
	private static final String UNSUPPORTED_TERM = "Unsupported term: %s";
	private static final String UNSUPPORTED_CONSTANT = "Unsupported constant: %s";

	private final ModelContext modelContext;
	private final Context context;

	public TermToExpr(ModelContext modelContext) {
		this.modelContext = modelContext;
		context = modelContext.getZ3Context();
	}

	// Use raw types to avoid having to check and capture specific sorts that are already validated by the Term data
	// structure and types.
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Expr<?> toExpr(Term<?> term, Tuple parameterTuple, ObjectIntMap<NodeVariable> parameterMap) {
		return switch (term) {
			case ConstantTerm<?> constantTerm -> toExpr(constantTerm);
			case PartialFunctionCallTerm<?, ?> partialFunctionCallTerm ->
					toExpr(partialFunctionCallTerm, parameterTuple, parameterMap);
			case UnaryTerm<?, ?> unaryTerm -> {
				var body = (Expr) toExpr(unaryTerm.getBody(), parameterTuple, parameterMap);
				yield switch (unaryTerm) {
					case NotTerm<?> _ -> context.mkNot(body);
					case PlusTerm<?> _ -> body;
					case MinusTerm<?> _ -> context.mkUnaryMinus(body);
					case IntToRealTerm _ -> context.mkInt2Real(body);
					case RealToIntTerm _ -> context.mkReal2Int(body);
					default -> throw new IllegalArgumentException(UNSUPPORTED_TERM.formatted(unaryTerm));
				};
			}
			case BinaryTerm<?, ?, ?> binaryTerm -> {
				var x = (Expr) toExpr(binaryTerm.getLeft(), parameterTuple, parameterMap);
				var y = (Expr) toExpr(binaryTerm.getRight(), parameterTuple, parameterMap);
				yield switch (binaryTerm) {
					case AbstractDomainEqTerm<?, ?> _ -> context.mkEq(x, y);
					case AbstractDomainNotEqTerm<?, ?> _ -> context.mkNot(context.mkEq(x, y));
					case AbstractDomainGreaterEqTerm<?, ?> _ -> context.mkGe(x, y);
					case AbstractDomainGreaterTerm<?, ?> _ -> context.mkGt(x, y);
					case AbstractDomainLessEqTerm<?, ?> _ -> context.mkLe(x, y);
					case AbstractDomainLessTerm<?, ?> _ -> context.mkLt(x, y);
					case AndTerm<?> _ -> context.mkAnd(x, y);
					case OrTerm<?> _ -> context.mkOr(x, y);
					case XorTerm<?> _ -> context.mkXor(x, y);
					case AddTerm<?> _ -> {
						if (StringValue.class.equals(term.getType())) {
							yield context.mkConcat(new Expr[]{x, y});
						} else {
							yield context.mkAdd(x, y);
						}
					}
					case SubTerm<?> _ -> context.mkSub(x, y);
					case MulTerm<?> _ -> context.mkMul(x, y);
					case DivTerm<?> _ -> context.mkDiv(x, y);
					case PowTerm<?> _ -> context.mkPower(x, y);
					default -> throw new IllegalArgumentException(UNSUPPORTED_TERM.formatted(binaryTerm));
				};
			}
			default -> throw new IllegalArgumentException(UNSUPPORTED_TERM.formatted(term));
		};
	}

	public Expr<?> toExpr(ConstantTerm<?> term) {
		var value = term.getValue();
		if (!(value instanceof AbstractValue<?, ?> abstractValue) || !abstractValue.isConcrete()) {
			throw new IllegalArgumentException(UNSUPPORTED_CONSTANT.formatted(value));
		}
		var concreteValue = abstractValue.getArbitrary();
		return switch (concreteValue) {
			case Boolean booleanValue -> context.mkBool(booleanValue);
			case BigInteger intValue -> context.mkInt(intValue.toString(10));
			case BigDecimal realValue -> context.mkReal(realValue.toString());
			case String stringValue -> context.mkString(stringValue);
			case null, default -> throw new IllegalArgumentException(UNSUPPORTED_CONSTANT.formatted(abstractValue));
		};
	}

	public Expr<?> toExpr(PartialFunctionCallTerm<?, ?> partialFunctionCallTerm, Tuple parameterTuple,
	                      ObjectIntMap<NodeVariable> parameterMap) {
		if (partialFunctionCallTerm.getConcreteness() != ConcretenessSpecification.UNSPECIFIED) {
			throw new IllegalArgumentException(
					"Partial function call with specified concreteness: " + partialFunctionCallTerm);
		}
		var arguments = partialFunctionCallTerm.getArguments();
		var argumentsArray = new int[arguments.size()];
		for (int i = 0; i < argumentsArray.length; i++) {
			argumentsArray[i] = parameterTuple.get(parameterMap.get(arguments.get(i)));
		}
		var argumentsTuple = Tuple.of(argumentsArray);
		var declaration = modelContext.getVariable(partialFunctionCallTerm.getPartialFunction(), argumentsTuple);
		return context.mkConst(declaration);
	}
}
