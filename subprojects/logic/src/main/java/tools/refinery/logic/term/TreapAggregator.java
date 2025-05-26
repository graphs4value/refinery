/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import java.util.function.BinaryOperator;

public class TreapAggregator<R, T extends Comparable<? super T>> implements StatefulAggregator<R, T> {
	private final Class<R> resultType;
	private final Class<T> inputType;
	private final ToMonoid<T, R> toMonoid;
	private final R unit;
	private final BinaryOperator<R> monoid;

	public TreapAggregator(Class<R> resultType, Class<T> inputType, ToMonoid<T, R> toMonoid, R unit,
						   BinaryOperator<R> monoid) {
		this.resultType = resultType;
		this.inputType = inputType;
		this.toMonoid = toMonoid;
		this.unit = unit;
		this.monoid = monoid;
	}

	@Override
	public StatefulAggregate<R, T> createEmptyAggregate() {
		return new TreapAggregate<>(toMonoid, unit, monoid);
	}

	@Override
	public Class<R> getResultType() {
		return resultType;
	}

	@Override
	public Class<T> getInputType() {
		return inputType;
	}

	public static <R, T extends Comparable<? super T>> TreapAggregator<R, T> of(
			Class<R> resultType, Class<T> inputType, ToMonoid<T, R> toMonoid, R unit, BinaryOperator<R> monoid) {
		return new TreapAggregator<>(resultType, inputType, toMonoid, unit, monoid);
	}

	public static <T extends Comparable<? super T>> TreapAggregator<T, T> of(
			Class<T> inputType, ToMonoid<T, T> toMonoid, T unit, BinaryOperator<T> monoid) {
		return of(inputType, inputType, toMonoid, unit, monoid);
	}
}
