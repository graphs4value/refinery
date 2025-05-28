/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.expressions;

import tools.refinery.language.model.problem.BinaryOp;
import tools.refinery.language.model.problem.ComparisonOp;
import tools.refinery.language.model.problem.UnaryOp;
import tools.refinery.language.typesystem.AggregatorName;
import tools.refinery.language.typesystem.DataExprType;
import tools.refinery.logic.AnyAbstractDomain;
import tools.refinery.logic.term.AnyTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.truthvalue.TruthValue;

import java.util.*;
import java.util.function.Function;

// This class is used to configure term interpreters by clients with various arguments.
@SuppressWarnings("SameParameterValue")
public abstract class AbstractTermInterpreter implements TermInterpreter {
	private final Map<DataExprType, UnaryValue<?, ?>> negations = new HashMap<>();
	private final Map<UnaryKey, DataExprType> unaryOperators = new HashMap<>();
	private final Map<DataExprType, Comparison<?>> comparisons = new HashMap<>();
	private final Map<DataExprType, Range<?>> ranges = new HashMap<>();
	private final Map<DataExprType, BinaryOperator<?>> binaryOperatorTerms = new HashMap<>();
	private final Map<BinaryKey, DataExprType> binaryOperators = new HashMap<>();
	private final Set<CastKey> casts = new HashSet<>();
	private final Map<AggregatorKey, DataExprType> aggregators = new HashMap<>();
	private final Map<DataExprType, AnyAbstractDomain> domains = new HashMap<>();

	protected AbstractTermInterpreter() {
	}

	protected <R, T> void addNegation(DataExprType type, DataExprType result, Function<Term<R>, Term<T>> termFactory) {
		negations.put(type, new UnaryValue<>(result, termFactory));
	}

	protected <T> void addNegation(DataExprType type, Function<Term<T>, Term<T>> termFactory) {
		addNegation(type, type, termFactory);
	}

	protected void addUnaryOperator(UnaryOp op, DataExprType type, DataExprType result) {
		unaryOperators.put(new UnaryKey(op, type), result);
	}

	protected void addUnaryOperator(UnaryOp op, DataExprType type) {
		addUnaryOperator(op, type, type);
	}

	protected <T> void addComparison(DataExprType type, Comparison<T> comparison) {
		comparisons.put(type, comparison);
	}

	protected <T> void addBinaryOperator(DataExprType type, BinaryOperator<T> operator) {
		binaryOperatorTerms.put(type, operator);
	}

	protected void addDomain(DataExprType type, AnyAbstractDomain domain) {
		domains.put(type, domain);
	}

	protected <T> void addRange(DataExprType type, Range<T> range) {
		ranges.put(type, range);
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
		return Optional.ofNullable(negations.get(type).resultType());
	}

	@Override
	public Optional<AnyAbstractDomain> getDomain(DataExprType type) {
		return Optional.ofNullable(domains.get(type));
	}

	@Override
	public Optional<AnyTerm> createNegation(DataExprType type, AnyTerm body) {
		var value = negations.get(type);
		if (value == null) {
			return Optional.empty();
		}
		return Optional.of(value.createTerm(body));
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
		return comparisons.containsKey(type);
	}

	@Override
	public Optional<Term<TruthValue>> createComparison(ComparisonOp op, DataExprType type, AnyTerm left,
													   AnyTerm right) {
		var value = comparisons.get(type);
		if (value == null) {
			return Optional.empty();
		}
		return Optional.of(value.toComparisonUnchecked(op, left, right));
	}

	@Override
	public Optional<AnyTerm> createBinaryOperator(BinaryOp op, DataExprType type, AnyTerm left, AnyTerm right) {
		var value = binaryOperatorTerms.get(type);
		if (value == null) {
			return Optional.empty();
		}
		return Optional.of(value.toBinaryOperatorUnchecked(op, left, right));
	}

	@Override
	public Optional<AnyTerm> createRange(DataExprType type, AnyTerm left, AnyTerm right) {
		var value = ranges.get(type);
		if (value == null) {
			return Optional.empty();
		}
		return Optional.of(value.toRangeUnchecked(left, right));
	}

	@Override
	public boolean isRangeSupported(DataExprType type) {
		return ranges.containsKey(type);
	}

	@Override
	public Optional<DataExprType> getBinaryOperatorType(BinaryOp op, DataExprType leftType, DataExprType rightType) {
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

	private record UnaryValue<R, T>(DataExprType resultType, Function<Term<T>, Term<R>> termFactory) {
		public AnyTerm createTerm(AnyTerm body) {
			// This is safe, because the constructor of the term created by {@code termFactory} will always check the
			// runtime type of the term, avoiding heap pollution.
			@SuppressWarnings("unchecked")
			var uncheckedBody = (Term<T>) body;
			return termFactory.apply(uncheckedBody);
		}
	}

	@FunctionalInterface
	public interface Comparison<T> {
		Term<TruthValue> toComparison(ComparisonOp op, Term<T> left, Term<T> right);

		default Term<TruthValue> toComparisonUnchecked(ComparisonOp op, AnyTerm left, AnyTerm right) {
			// This is safe, because the constructor of the term created by {@code toComparison} will always check the
			// runtime type of the term, avoiding heap pollution.
			@SuppressWarnings("unchecked")
			var uncheckedLeft = (Term<T>) left;
			@SuppressWarnings("unchecked")
			var uncheckedRight = (Term<T>) right;
			return toComparison(op, uncheckedLeft, uncheckedRight);
		}
	}

	@FunctionalInterface
	public interface BinaryOperator<T> {
		Term<T> toBinaryOperator(BinaryOp op, Term<T> left, Term<T> right);

		default Term<T> toBinaryOperatorUnchecked(BinaryOp op, AnyTerm left, AnyTerm right) {
			// This is safe, because the constructor of the term created by {@code toComparison} will always check the
			// runtime type of the term, avoiding heap pollution.
			@SuppressWarnings("unchecked")
			var uncheckedLeft = (Term<T>) left;
			@SuppressWarnings("unchecked")
			var uncheckedRight = (Term<T>) right;
			return toBinaryOperator(op, uncheckedLeft, uncheckedRight);
		}
	}

	@FunctionalInterface
	public interface Range<T> {
		Term<T> toRange(Term<T> left, Term<T> right);

		default Term<T> toRangeUnchecked(AnyTerm left, AnyTerm right) {
			// This is safe, because the constructor of the term created by {@code toComparison} will always check the
			// runtime type of the term, avoiding heap pollution.
			@SuppressWarnings("unchecked")
			var uncheckedLeft = (Term<T>) left;
			@SuppressWarnings("unchecked")
			var uncheckedRight = (Term<T>) right;
			return toRange(uncheckedLeft, uncheckedRight);
		}
	}
}
