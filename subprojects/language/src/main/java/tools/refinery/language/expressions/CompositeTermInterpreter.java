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

import java.util.List;
import java.util.Optional;

public class CompositeTermInterpreter implements TermInterpreter {
	private final List<TermInterpreter> interpreters;

    public CompositeTermInterpreter(List<TermInterpreter> interpreters) {
        this.interpreters = interpreters;
    }

	@Override
	public Optional<DataExprType> getNegationType(DataExprType type) {
		for (var interpreter : interpreters) {
			var result = interpreter.getNegationType(type);
			if (result.isPresent()) {
				return result;
			}
		}
		return Optional.empty();
	}

	@Override
	public Optional<DataExprType> getUnaryOperationType(UnaryOp op, DataExprType type) {
		for (var interpreter : interpreters) {
			var result = interpreter.getUnaryOperationType(op, type);
			if (result.isPresent()) {
				return result;
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean isComparisonSupported(DataExprType type) {
		for (var interpreter : interpreters) {
			var result = interpreter.isComparisonSupported(type);
			if (result) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isRangeSupported(DataExprType type) {
		for (var interpreter : interpreters) {
			var result = interpreter.isRangeSupported(type);
			if (result) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Optional<DataExprType> getBinaryOperationType(BinaryOp op, DataExprType leftType, DataExprType rightType) {
		for (var interpreter : interpreters) {
			var result = interpreter.getBinaryOperationType(op, leftType, rightType);
			if (result.isPresent()) {
				return result;
			}
		}
		return Optional.empty();
	}

	@Override
	public boolean isCastSupported(DataExprType fromType, DataExprType toType) {
		for (var interpreter : interpreters) {
			var result = interpreter.isCastSupported(fromType, toType);
			if (result) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Optional<DataExprType> getAggregationType(AggregatorName aggregator, DataExprType type) {
		for (var interpreter : interpreters) {
			var result = interpreter.getAggregationType(aggregator, type);
			if (result.isPresent()) {
				return result;
			}
		}
		return Optional.empty();
	}
}
