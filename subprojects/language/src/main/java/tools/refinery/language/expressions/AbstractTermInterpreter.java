/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.expressions;

import tools.refinery.language.model.problem.BinaryOp;
import tools.refinery.language.model.problem.UnaryOp;
import tools.refinery.language.typesystem.AggregatorName;
import tools.refinery.language.typesystem.DataExprType;

import java.util.*;

// This class is used to configure term interpreters by clients with various arguments.
@SuppressWarnings("SameParameterValue")
public abstract class AbstractTermInterpreter implements TermInterpreter {
	private final Map<DataExprType, DataExprType> negations = new HashMap<>();
	private final Map<UnaryKey, DataExprType> unaryOperators = new HashMap<>();
	private final Set<DataExprType> comparisons = new HashSet<>();
	private final Set<DataExprType> ranges = new HashSet<>();
	private final Map<BinaryKey, DataExprType> binaryOperators = new HashMap<>();
	private final Set<CastKey> casts = new HashSet<>();
	private final Map<AggregatorKey, DataExprType> aggregators = new HashMap<>();

	protected AbstractTermInterpreter() {
	}

	protected void addNegation(DataExprType type, DataExprType result) {
		negations.put(type, result);
	}

	protected void addNegation(DataExprType type) {
		addNegation(type, type);
	}

	protected void addUnaryOperator(UnaryOp op, DataExprType type, DataExprType result) {
		unaryOperators.put(new UnaryKey(op, type), result);
	}

	protected void addUnaryOperator(UnaryOp op, DataExprType type) {
		addUnaryOperator(op, type, type);
	}

	protected void addComparison(DataExprType type) {
		comparisons.add(type);
	}

	protected void addRange(DataExprType type) {
		ranges.add(type);
	}

	protected void addBinaryOperator(BinaryOp op, DataExprType leftType, DataExprType rightType, DataExprType result) {
		binaryOperators.put(new BinaryKey(op, leftType, rightType), result);
	}

	protected void addBinaryOperator(BinaryOp op, DataExprType type) {
		addBinaryOperator(op, type, type, type);
	}

	protected void addCast(DataExprType fromType, DataExprType toType) {
		if (fromType.equals(toType)) {
			throw new IllegalArgumentException("The fromType and toType of a cast operator must be different");
		}
		casts.add(new CastKey(fromType, toType));
	}

	protected void addAggregator(AggregatorName aggregator, DataExprType type, DataExprType result) {
		aggregators.put(new AggregatorKey(aggregator, type), result);
	}

	protected void addAggregator(AggregatorName aggregator, DataExprType type) {
		addAggregator(aggregator, type, type);
	}

	@Override
	public Optional<DataExprType> getNegationType(DataExprType type) {
		return Optional.ofNullable(negations.get(type));
	}

	@Override
	public Optional<DataExprType> getUnaryOperationType(UnaryOp op, DataExprType type) {
		if (unaryOperators.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(unaryOperators.get(new UnaryKey(op, type)));
	}

	@Override
	public boolean isComparisonSupported(DataExprType type) {
		return comparisons.contains(type);
	}

	@Override
	public boolean isRangeSupported(DataExprType type) {
		return ranges.contains(type);
	}

	@Override
	public Optional<DataExprType> getBinaryOperationType(BinaryOp op, DataExprType leftType, DataExprType rightType) {
		if (binaryOperators.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(binaryOperators.get(new BinaryKey(op, leftType, rightType)));
	}

	@Override
	public Optional<DataExprType> getAggregationType(AggregatorName aggregator, DataExprType type) {
		if (aggregators.isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(aggregators.get(new AggregatorKey(aggregator, type)));
	}

	@Override
	public boolean isCastSupported(DataExprType fromType, DataExprType toType) {
		return casts.contains(new CastKey(fromType, toType));
	}

	private record UnaryKey(UnaryOp op, DataExprType type) {
	}

	private record BinaryKey(BinaryOp op, DataExprType leftType, DataExprType rightType) {
	}

	private record CastKey(DataExprType fromType, DataExprType toType) {
	}

	private record AggregatorKey(AggregatorName aggregator, DataExprType type) {
	}
}
