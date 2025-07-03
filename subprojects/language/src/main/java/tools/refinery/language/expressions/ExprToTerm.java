/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.expressions;

import com.google.inject.Inject;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.scoping.imports.ImportAdapterProvider;
import tools.refinery.language.typesystem.DataExprType;
import tools.refinery.language.typesystem.ProblemTypeAnalyzer;
import tools.refinery.logic.term.AnyTerm;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalTerms;

import java.util.Optional;

public class ExprToTerm {
	@Inject
	private ImportAdapterProvider importAdapterProvider;

	@Inject
	private ProblemTypeAnalyzer typeAnalyzer;

	public Optional<AnyTerm> toTerm(Expr expr) {
		return switch (expr) {
			case NegationExpr negationExpr -> createNegation(negationExpr);
			case ArithmeticUnaryExpr arithmeticUnaryExpr -> createUnaryOperator(arithmeticUnaryExpr);
			case ArithmeticBinaryExpr arithmeticBinaryExpr -> createBinaryOperator(arithmeticBinaryExpr);
			case ComparisonExpr comparisonExpr -> createComparison(comparisonExpr);
			case RangeExpr rangeExpr -> createRange(rangeExpr);
			case LogicConstant logicConstant -> createLogicConstant(logicConstant);
			case IntConstant intConstant -> createIntConstant(intConstant);
			case null, default -> Optional.empty();
		};
	}

	private Optional<AnyTerm> createNegation(NegationExpr expr) {
		var body = expr.getBody();
		if (!(typeAnalyzer.getExpressionType(body) instanceof DataExprType bodyType)) {
			return Optional.empty();
		}
		var termInterpreter = importAdapterProvider.getTermInterpreter(expr);
		return toTerm(body).flatMap(bodyTerm ->
				termInterpreter.createNegation(bodyType, bodyTerm));
	}

	private Optional<AnyTerm> createUnaryOperator(ArithmeticUnaryExpr expr) {
		var body = expr.getBody();
		if (!(typeAnalyzer.getExpressionType(body) instanceof DataExprType bodyType)) {
			return Optional.empty();
		}
		var termInterpreter = importAdapterProvider.getTermInterpreter(expr);
		return toTerm(body).flatMap(bodyTerm ->
				termInterpreter.createUnaryOperator(expr.getOp(), bodyType, bodyTerm));
	}

	private Optional<AnyTerm> createBinaryOperator(ArithmeticBinaryExpr expr) {
		var left = expr.getLeft();
		var right = expr.getRight();
		if (!(typeAnalyzer.getExpressionType(left) instanceof DataExprType leftType) ||
				!(typeAnalyzer.getExpressionType(right) instanceof DataExprType rightType)) {
			return Optional.empty();
		}
		var termInterpreter = importAdapterProvider.getTermInterpreter(expr);
		return toTerm(left).flatMap(leftTerm ->
				toTerm(right).flatMap(rightTerm -> termInterpreter.createBinaryOperator(
						expr.getOp(), leftType, rightType, leftTerm, rightTerm)));
	}

	private Optional<AnyTerm> createComparison(ComparisonExpr expr) {
		var left = expr.getLeft();
		if (!(typeAnalyzer.getExpressionType(left) instanceof DataExprType leftType)) {
			return Optional.empty();
		}
		var termInterpreter = importAdapterProvider.getTermInterpreter(expr);
		var right = expr.getRight();
		return toTerm(left).flatMap(leftTerm ->
				toTerm(right).flatMap(rightTerm -> termInterpreter.createComparison(
						expr.getOp(), leftType, leftTerm, rightTerm)));
	}

	private Optional<AnyTerm> createRange(RangeExpr expr) {
		var left = expr.getLeft();
		if (!(typeAnalyzer.getExpressionType(left) instanceof DataExprType leftType)) {
			return Optional.empty();
		}
		var right = expr.getRight();
		var termInterpreter = importAdapterProvider.getTermInterpreter(expr);
		Optional<AnyTerm> maybeLeftTerm;
		if (left instanceof InfiniteConstant) {
			if (right instanceof InfiniteConstant) {
				return termInterpreter.createUnknown(leftType);
			}
			maybeLeftTerm = termInterpreter.createNegativeInfinity(leftType);
		} else {
			maybeLeftTerm = toTerm(left);
		}
		var maybeRightTerm = right instanceof InfiniteConstant ? termInterpreter.createPositiveInfinity(leftType) :
				toTerm(right);
		return maybeLeftTerm.flatMap(leftTerm ->
				maybeRightTerm.flatMap(rightTerm ->
						termInterpreter.createRange(leftType, leftTerm, rightTerm)));
	}

	private Optional<AnyTerm> createLogicConstant(LogicConstant expr) {
		if (!(typeAnalyzer.getExpressionType(expr) instanceof DataExprType type)) {
			return Optional.empty();
		}
		var termInterpreter = importAdapterProvider.getTermInterpreter(expr);
		return switch (expr.getLogicValue()) {
			case UNKNOWN -> termInterpreter.createUnknown(type);
			case TRUE -> termInterpreter.createPositiveInfinity(type);
			case FALSE -> termInterpreter.createNegativeInfinity(type);
			case ERROR -> termInterpreter.createError(type);
			case null -> Optional.empty();
		};
	}

	private static Optional<AnyTerm> createIntConstant(IntConstant expr) {
		return Optional.of(IntIntervalTerms.constant(IntInterval.of(expr.getIntValue())));
	}
}
