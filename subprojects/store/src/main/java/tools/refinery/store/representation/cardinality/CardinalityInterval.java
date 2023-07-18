/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation.cardinality;

public sealed interface CardinalityInterval permits NonEmptyCardinalityInterval, EmptyCardinalityInterval {
	int lowerBound();

	UpperCardinality upperBound();

	boolean isEmpty();

	CardinalityInterval min(CardinalityInterval other);

	CardinalityInterval max(CardinalityInterval other);

	CardinalityInterval add(CardinalityInterval other);

	CardinalityInterval take(int count);

	CardinalityInterval multiply(CardinalityInterval other);

	CardinalityInterval meet(CardinalityInterval other);

	CardinalityInterval join(CardinalityInterval other);
}
