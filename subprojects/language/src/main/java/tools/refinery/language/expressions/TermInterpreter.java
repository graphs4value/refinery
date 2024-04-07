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

import java.util.Optional;

public interface TermInterpreter {
	Optional<DataExprType> getNegationType(DataExprType type);

	Optional<DataExprType> getUnaryOperationType(UnaryOp op, DataExprType type);

	boolean isComparisonSupported(DataExprType type);

	boolean isRangeSupported(DataExprType type);

	Optional<DataExprType> getBinaryOperationType(BinaryOp op, DataExprType leftType, DataExprType rightType);

	boolean isCastSupported(DataExprType fromType, DataExprType toType);

	Optional<DataExprType> getAggregationType(AggregatorName aggregator, DataExprType type);
}
