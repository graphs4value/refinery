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
		if (expr == null) {
			return Optional.empty();
		}
		var termInterpreter = importAdapterProvider.getTermInterpreter(expr);
		return switch (expr) {
			case NegationExpr negationExpr -> {
				var body = negationExpr.getBody();
				if (!(typeAnalyzer.getExpressionType(body) instanceof DataExprType bodyType)) {
					yield Optional.empty();
				}
				yield toTerm(body).flatMap(bodyTerm ->
						termInterpreter.createNegation(bodyType, bodyTerm));
			}
			case ComparisonExpr comparisonExpr -> {
				var left = comparisonExpr.getLeft();
				if (!(typeAnalyzer.getExpressionType(left) instanceof DataExprType leftType)) {
					yield Optional.empty();
				}
				var right = comparisonExpr.getRight();
				yield toTerm(left).flatMap(leftTerm ->
						toTerm(right).flatMap(rightTerm -> termInterpreter.createComparison(
								comparisonExpr.getOp(), leftType, leftTerm, rightTerm)));
			}
			case IntConstant intConstant -> {
				if (!(typeAnalyzer.getExpressionType(intConstant) instanceof DataExprType)) {
					yield Optional.empty();
				} else {
					yield Optional.of(IntIntervalTerms.constant(IntInterval.of(intConstant.getIntValue())));
				}
			}
			case RangeExpr rangeExpr -> {
				var left = rangeExpr.getLeft();
				var right = rangeExpr.getRight();
				if (!(typeAnalyzer.getExpressionType(left) instanceof DataExprType leftType)) {
					yield Optional.empty();
				}
				yield toTerm(left).flatMap(leftTerm ->
						toTerm(right).flatMap(rightTerm ->
								termInterpreter.createRange(leftType, leftTerm, rightTerm)));
			}
			case ArithmeticBinaryExpr arithmeticBinaryExpr -> {
				var left = arithmeticBinaryExpr.getLeft();
				if (!(typeAnalyzer.getExpressionType(left) instanceof DataExprType leftType)) {
					yield Optional.empty();
				}
				var right = arithmeticBinaryExpr.getRight();
				yield toTerm(left).flatMap(leftTerm ->
						toTerm(right).flatMap(rightTerm -> termInterpreter.createBinaryOperator(
								arithmeticBinaryExpr.getOp(), leftType, leftTerm, rightTerm)));
			}
			default -> Optional.empty();
		};
	}
}
